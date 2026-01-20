package com.xhhao.aimodelhub.service.common;

import com.xhhao.aimodelhub.api.ChatMessage;
import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.extension.AiChatLog;
import com.xhhao.aimodelhub.service.openai.OpenAiCompatibleChatModel;
import com.xhhao.aimodelhub.service.openai.OpenAiChatRequest;
import com.xhhao.aimodelhub.service.openai.OpenAiChatResponse;
import com.xhhao.aimodelhub.service.openai.OpenAiMessage;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 带日志记录的 ChatModel 包装器
 * <p>
 * 使用装饰器模式，透明地为 AI 调用添加日志记录功能。
 * 日志记录完全异步，不阻塞主流程。
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
@Slf4j
public class LoggingChatModel implements ChatModel {

    private final OpenAiCompatibleChatModel delegate;
    private final AiChatLogService logService;
    private final String callerPlugin;
    private final String provider;

    public LoggingChatModel(OpenAiCompatibleChatModel delegate, AiChatLogService logService,
                            String callerPlugin, String provider) {
        this.delegate = delegate;
        this.logService = logService;
        this.callerPlugin = callerPlugin;
        this.provider = provider;
    }

    @Override
    public Mono<String> chat(String userMessage) {
        return chat(List.of(ChatMessage.user(userMessage)));
    }

    @Override
    public Flux<String> chatStream(String userMessage) {
        return chatStream(List.of(ChatMessage.user(userMessage)));
    }

    @Override
    public Mono<String> chat(List<ChatMessage> messages) {
        long startTime = System.currentTimeMillis();
        String userMessage = extractLastUserMessage(messages);
        OpenAiChatRequest request = buildRequest(messages, false);

        return delegate.chat(request)
            .doOnSuccess(response -> asyncLogSuccess(userMessage, AiChatLog.CallType.CHAT, startTime, response))
            .doOnError(e -> asyncLogError(userMessage, AiChatLog.CallType.CHAT, startTime, e))
            .map(OpenAiChatResponse::getContent);
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages) {
        long startTime = System.currentTimeMillis();
        String userMessage = extractLastUserMessage(messages);
        OpenAiChatRequest request = buildRequest(messages, true);

        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<OpenAiChatResponse.Usage> usageRef = new AtomicReference<>();

        return delegate.chatStream(request)
            .doOnNext(response -> collectStreamResponse(response, fullResponse, usageRef))
            .filter(response -> response.getContent() != null && !response.getContent().isEmpty())
            .map(OpenAiChatResponse::getContent)
            .doOnComplete(() -> asyncLogStreamSuccess(userMessage, startTime, fullResponse.toString(), usageRef.get()))
            .doOnError(e -> asyncLogError(userMessage, AiChatLog.CallType.STREAM, startTime, e));
    }

    /**
     * 构建 OpenAI 请求
     */
    private OpenAiChatRequest buildRequest(List<ChatMessage> messages, boolean stream) {
        List<OpenAiMessage> openAiMessages = messages.stream()
            .map(msg -> new OpenAiMessage(msg.getRole(), msg.getContent(), null, null, null))
            .toList();

        OpenAiChatRequest.OpenAiChatRequestBuilder builder = OpenAiChatRequest.builder()
            .model(delegate.getModelName())
            .messages(openAiMessages);

        if (stream) {
            builder.stream(true)
                .streamOptions(new OpenAiChatRequest.StreamOptions(true));
        }

        return builder.build();
    }

    /**
     * 收集流式响应数据
     */
    private void collectStreamResponse(OpenAiChatResponse response, StringBuilder fullResponse,
                                       AtomicReference<OpenAiChatResponse.Usage> usageRef) {
        String content = response.getContent();
        if (content != null) {
            fullResponse.append(content);
        }
        if (response.getUsage() != null) {
            usageRef.set(response.getUsage());
        }
    }

    /**
     * 异步记录成功日志
     */
    private void asyncLogSuccess(String userMessage, AiChatLog.CallType callType,
                                 long startTime, OpenAiChatResponse response) {
        executeAsync(() -> {
            String content = response != null ? response.getContent() : null;
            OpenAiChatResponse.Usage usage = response != null ? response.getUsage() : null;
            Integer promptTokens = usage != null ? usage.getPromptTokens() : null;
            Integer completionTokens = usage != null ? usage.getCompletionTokens() : null;

            logService.logChat(callerPlugin, provider, delegate.getModelName(),
                    userMessage, callType, startTime, promptTokens, completionTokens,
                    true, null, content)
                .subscribe(
                    saved -> log.debug("日志已保存: {}", saved.getMetadata().getName()),
                    e -> log.warn("保存日志失败", e)
                );
        });
    }

    /**
     * 异步记录流式成功日志
     */
    private void asyncLogStreamSuccess(String userMessage, long startTime,
                                       String fullResponse, OpenAiChatResponse.Usage usage) {
        executeAsync(() -> {
            Integer promptTokens = usage != null ? usage.getPromptTokens() : null;
            Integer completionTokens = usage != null ? usage.getCompletionTokens() : null;

            logService.logChat(callerPlugin, provider, delegate.getModelName(),
                    userMessage, AiChatLog.CallType.STREAM, startTime, promptTokens, completionTokens,
                    true, null, fullResponse)
                .subscribe(
                    saved -> log.debug("流式日志已保存: {}", saved.getMetadata().getName()),
                    e -> log.warn("保存流式日志失败", e)
                );
        });
    }

    /**
     * 异步记录错误日志
     */
    private void asyncLogError(String userMessage, AiChatLog.CallType callType,
                               long startTime, Throwable error) {
        executeAsync(() -> logService.logChat(callerPlugin, provider, delegate.getModelName(),
                userMessage, callType, startTime, null, null,
                false, error.getMessage(), null)
            .subscribe(
                saved -> log.debug("错误日志已保存: {}", saved.getMetadata().getName()),
                e -> log.warn("保存错误日志失败", e)
            ));
    }

    /**
     * 在弹性线程池中异步执行任务
     */
    private void executeAsync(Runnable task) {
        Mono.fromRunnable(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("异步任务执行失败", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * 提取最后一条用户消息
     */
    private String extractLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (ChatMessage.Role.USER.getValue().equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return null;
    }
}
