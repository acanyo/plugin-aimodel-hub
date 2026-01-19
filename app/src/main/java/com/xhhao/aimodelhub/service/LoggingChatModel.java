package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ChatMessage;
import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.extension.AiChatLog;
import com.xhhao.aimodelhub.service.openai.OpenAiChatModel;
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
 * 日志记录完全异步，不阻塞主流程
 * </p>
 *
 * @author Handsome
 */
@Slf4j
public class LoggingChatModel implements ChatModel {

    private final OpenAiChatModel delegate;
    private final AiChatLogService logService;
    private final String callerPlugin;
    private final String provider;

    public LoggingChatModel(OpenAiChatModel delegate, AiChatLogService logService, 
                           String callerPlugin, String provider) {
        this.delegate = delegate;
        this.logService = logService;
        this.callerPlugin = callerPlugin;
        this.provider = provider;
    }

    @Override
    public Mono<String> chat(String userMessage) {
        long startTime = System.currentTimeMillis();
        
        OpenAiChatRequest request = OpenAiChatRequest.builder()
            .model(delegate.getModelName())
            .messages(List.of(OpenAiMessage.user(userMessage)))
            .build();
        
        return delegate.chat(request)
            .doOnSuccess(response -> {
                asyncLogChat(userMessage, AiChatLog.CallType.CHAT, startTime, response, null);
            })
            .doOnError(e -> {
                // 异步记录错误日志
                asyncLogError(userMessage, AiChatLog.CallType.CHAT, startTime, e);
            })
            .map(OpenAiChatResponse::getContent);
    }

    @Override
    public Flux<String> chatStream(String userMessage) {
        long startTime = System.currentTimeMillis();
        
        OpenAiChatRequest request = OpenAiChatRequest.builder()
            .model(delegate.getModelName())
            .messages(List.of(OpenAiMessage.user(userMessage)))
            .stream(true)
            .streamOptions(new OpenAiChatRequest.StreamOptions(true))
            .build();
        
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<OpenAiChatResponse.Usage> usageRef = new AtomicReference<>();
        
        return delegate.chatStream(request)
            .doOnNext(response -> {
                String content = response.getContent();
                if (content != null) {
                    fullResponse.append(content);
                }
                if (response.getUsage() != null) {
                    usageRef.set(response.getUsage());
                }
            })
            .filter(response -> response.getContent() != null && !response.getContent().isEmpty())
            .map(OpenAiChatResponse::getContent)
            .doOnComplete(() -> {
                // 异步记录日志
                asyncLogStreamChat(userMessage, startTime, fullResponse.toString(), usageRef.get());
            })
            .doOnError(e -> {
                asyncLogError(userMessage, AiChatLog.CallType.STREAM, startTime, e);
            });
    }

    @Override
    public Mono<String> chat(List<ChatMessage> messages) {
        long startTime = System.currentTimeMillis();
        String userMessage = extractLastUserMessage(messages);
        
        List<OpenAiMessage> openAiMessages = messages.stream()
            .map(msg -> new OpenAiMessage(msg.getRole(), msg.getContent(), null, null, null))
            .toList();
        
        OpenAiChatRequest request = OpenAiChatRequest.builder()
            .model(delegate.getModelName())
            .messages(openAiMessages)
            .build();
        
        return delegate.chat(request)
            .doOnSuccess(response -> {
                asyncLogChat(userMessage, AiChatLog.CallType.CHAT, startTime, response, null);
            })
            .doOnError(e -> {
                asyncLogError(userMessage, AiChatLog.CallType.CHAT, startTime, e);
            })
            .map(OpenAiChatResponse::getContent);
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages) {
        long startTime = System.currentTimeMillis();
        String userMessage = extractLastUserMessage(messages);
        
        List<OpenAiMessage> openAiMessages = messages.stream()
            .map(msg -> new OpenAiMessage(msg.getRole(), msg.getContent(), null, null, null))
            .toList();
        
        OpenAiChatRequest request = OpenAiChatRequest.builder()
            .model(delegate.getModelName())
            .messages(openAiMessages)
            .stream(true)
            .streamOptions(new OpenAiChatRequest.StreamOptions(true))
            .build();
        
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<OpenAiChatResponse.Usage> usageRef = new AtomicReference<>();
        
        return delegate.chatStream(request)
            .doOnNext(response -> {
                String content = response.getContent();
                if (content != null) {
                    fullResponse.append(content);
                }
                if (response.getUsage() != null) {
                    usageRef.set(response.getUsage());
                }
            })
            .filter(response -> response.getContent() != null && !response.getContent().isEmpty())
            .map(OpenAiChatResponse::getContent)
            .doOnComplete(() -> {
                asyncLogStreamChat(userMessage, startTime, fullResponse.toString(), usageRef.get());
            })
            .doOnError(e -> {
                asyncLogError(userMessage, AiChatLog.CallType.STREAM, startTime, e);
            });
    }

    /**
     * 异步记录非流式请求日志
     */
    private void asyncLogChat(String userMessage, AiChatLog.CallType callType, long startTime, 
                             OpenAiChatResponse response, Throwable error) {
        Mono.fromRunnable(() -> {
            try {
                String content = response != null ? response.getContent() : null;
                OpenAiChatResponse.Usage usage = response != null ? response.getUsage() : null;
                Integer promptTokens = usage != null ? usage.getPromptTokens() : null;
                Integer completionTokens = usage != null ? usage.getCompletionTokens() : null;
                
                logService.logChat(callerPlugin, provider, delegate.getModelName(),
                        userMessage, callType, startTime, promptTokens, completionTokens,
                        error == null, error != null ? error.getMessage() : null, content)
                    .subscribe(
                        saved -> log.debug("Chat log saved: {}", saved.getMetadata().getName()),
                        e -> log.warn("Failed to save chat log", e)
                    );
            } catch (Exception e) {
                log.warn("Error in async log", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * 异步记录流式请求日志
     */
    private void asyncLogStreamChat(String userMessage, long startTime, 
                                   String fullResponse, OpenAiChatResponse.Usage usage) {
        Mono.fromRunnable(() -> {
            try {
                Integer promptTokens = usage != null ? usage.getPromptTokens() : null;
                Integer completionTokens = usage != null ? usage.getCompletionTokens() : null;
                
                logService.logChat(callerPlugin, provider, delegate.getModelName(),
                        userMessage, AiChatLog.CallType.STREAM, startTime, promptTokens, completionTokens,
                        true, null, fullResponse)
                    .subscribe(
                        saved -> log.debug("Stream chat log saved: {}", saved.getMetadata().getName()),
                        e -> log.warn("Failed to save stream chat log", e)
                    );
            } catch (Exception e) {
                log.warn("Error in async stream log", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * 异步记录错误日志
     */
    private void asyncLogError(String userMessage, AiChatLog.CallType callType, long startTime, Throwable error) {
        Mono.fromRunnable(() -> {
            try {
                logService.logChat(callerPlugin, provider, delegate.getModelName(),
                        userMessage, callType, startTime, null, null,
                        false, error.getMessage(), null)
                    .subscribe(
                        saved -> log.debug("Error log saved: {}", saved.getMetadata().getName()),
                        e -> log.warn("Failed to save error log", e)
                    );
            } catch (Exception e) {
                log.warn("Error in async error log", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * 提取最后一条用户消息
     */
    private String extractLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return null;
    }
}
