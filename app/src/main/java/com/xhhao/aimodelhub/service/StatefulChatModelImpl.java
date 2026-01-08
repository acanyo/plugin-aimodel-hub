package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ChatMessage;
import com.xhhao.aimodelhub.api.ChatModel;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 有状态聊天模型实现（自动管理对话历史）
 * <p>
 * 类似 LangChain4j 的 ChatMemory
 * </p>
 *
 * @author Handsome
 */
public class StatefulChatModelImpl implements ChatModel {

    private final ChatModel delegate;

    @Getter
    private final String systemPrompt;

    /**
     * 对话历史（不含 system 消息）
     */
    private final List<ChatMessage> history = new ArrayList<>();

    /**
     * 最大历史消息数（默认 20 条，防止内存溢出和 token 超限）
     */
    private int maxHistory = 20;

    /**
     * 最后活跃时间
     */
    @Getter
    private Instant lastActiveTime = Instant.now();

    public StatefulChatModelImpl(ChatModel delegate, String systemPrompt) {
        this.delegate = delegate;
        this.systemPrompt = systemPrompt;
    }

    public StatefulChatModelImpl(ChatModel delegate, String systemPrompt, int maxHistory) {
        this.delegate = delegate;
        this.systemPrompt = systemPrompt;
        this.maxHistory = maxHistory;
    }

    /**
     * 检查会话是否已过期
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 是否已过期
     */
    public boolean isExpired(long timeoutMinutes) {
        return Instant.now().isAfter(lastActiveTime.plusSeconds(timeoutMinutes * 60));
    }

    @Override
    public Mono<String> chat(String userMessage) {
        // 更新活跃时间
        lastActiveTime = Instant.now();

        // 1. 构建完整消息列表
        List<ChatMessage> messages = buildMessages(userMessage);

        // 2. 调用底层模型
        return delegate.chat(messages)
            .doOnNext(response -> {
                // 3. 保存到历史
                history.add(ChatMessage.user(userMessage));
                history.add(ChatMessage.assistant(response));
                // 4. 清理超出限制的历史
                trimHistory();
            });
    }

    @Override
    public Flux<String> chatStream(String userMessage) {
        // 更新活跃时间
        lastActiveTime = Instant.now();

        // 1. 构建完整消息列表
        List<ChatMessage> messages = buildMessages(userMessage);

        // 2. 先添加用户消息到历史
        history.add(ChatMessage.user(userMessage));

        // 3. 收集完整响应
        StringBuilder fullResponse = new StringBuilder();

        return delegate.chatStream(messages)
            .doOnNext(chunk -> fullResponse.append(chunk))
            .doOnComplete(() -> {
                // 4. 流式结束后保存 AI 回复
                history.add(ChatMessage.assistant(fullResponse.toString()));
                trimHistory();
            })
            .doOnError(e -> {
                // 出错时移除刚添加的用户消息
                if (!history.isEmpty()) {
                    history.remove(history.size() - 1);
                }
            });
    }

    @Override
    public Mono<String> chat(List<ChatMessage> messages) {
        // 直接委托给底层模型（不自动管理历史）
        return delegate.chat(messages);
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages) {
        // 直接委托给底层模型（不自动管理历史）
        return delegate.chatStream(messages);
    }

    /**
     * 获取对话历史
     */
    public List<ChatMessage> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * 清空对话历史
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * 设置最大历史消息数
     */
    public void setMaxHistory(int maxMessages) {
        this.maxHistory = maxMessages;
        trimHistory();
    }

    /**
     * 构建完整消息列表（system + history + 当前消息）
     */
    private List<ChatMessage> buildMessages(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 添加 system prompt
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(ChatMessage.system(systemPrompt));
        }

        // 2. 添加历史消息
        messages.addAll(history);

        // 3. 添加当前用户消息
        messages.add(ChatMessage.user(userMessage));

        return messages;
    }

    /**
     * 清理超出限制的历史消息
     */
    private void trimHistory() {
        if (maxHistory > 0 && history.size() > maxHistory) {
            // 保留最近的 maxHistory 条消息
            int removeCount = history.size() - maxHistory;
            for (int i = 0; i < removeCount; i++) {
                history.remove(0);
            }
        }
    }
}
