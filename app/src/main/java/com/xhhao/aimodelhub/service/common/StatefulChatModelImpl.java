package com.xhhao.aimodelhub.service.common;

import com.xhhao.aimodelhub.api.ChatMessage;
import com.xhhao.aimodelhub.api.ChatModel;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 有状态聊天模型实现（自动管理对话历史）
 * <p>
 * 自动维护对话上下文，支持多轮对话
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
public class StatefulChatModelImpl implements ChatModel {

    private static final int DEFAULT_MAX_HISTORY = 20;

    private final ChatModel delegate;

    @Getter
    private final String systemPrompt;

    /**
     * 对话历史（不含 system 消息），使用 LinkedList 便于头部删除
     */
    private final LinkedList<ChatMessage> history = new LinkedList<>();

    /**
     * 最大历史消息数
     */
    private final int maxHistory;

    /**
     * 最后活跃时间
     */
    @Getter
    private Instant lastActiveTime = Instant.now();

    public StatefulChatModelImpl(ChatModel delegate, String systemPrompt) {
        this(delegate, systemPrompt, DEFAULT_MAX_HISTORY);
    }

    public StatefulChatModelImpl(ChatModel delegate, String systemPrompt, int maxHistory) {
        this.delegate = delegate;
        this.systemPrompt = systemPrompt;
        this.maxHistory = maxHistory > 0 ? maxHistory : DEFAULT_MAX_HISTORY;
    }

    @Override
    public Mono<String> chat(String userMessage) {
        lastActiveTime = Instant.now();
        List<ChatMessage> messages = buildMessages(userMessage);

        return delegate.chat(messages)
            .doOnNext(response -> {
                history.add(ChatMessage.user(userMessage));
                history.add(ChatMessage.assistant(response));
                trimHistory();
            });
    }

    @Override
    public Flux<String> chatStream(String userMessage) {
        lastActiveTime = Instant.now();
        List<ChatMessage> messages = buildMessages(userMessage);
        history.add(ChatMessage.user(userMessage));

        StringBuilder fullResponse = new StringBuilder();

        return delegate.chatStream(messages)
            .doOnNext(fullResponse::append)
            .doOnComplete(() -> {
                history.add(ChatMessage.assistant(fullResponse.toString()));
                trimHistory();
            })
            .doOnError(e -> {
                if (!history.isEmpty()) {
                    history.removeLast();
                }
            });
    }

    @Override
    public Mono<String> chat(List<ChatMessage> messages) {
        return delegate.chat(messages);
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages) {
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
     * 构建完整消息列表（system + history + 当前消息）
     */
    private List<ChatMessage> buildMessages(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        messages.addAll(history);
        messages.add(ChatMessage.user(userMessage));

        return messages;
    }

    /**
     * 清理超出限制的历史消息
     */
    private void trimHistory() {
        while (history.size() > maxHistory) {
            history.removeFirst();
        }
    }
}
