package com.xhhao.aimodelhub.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 聊天模型接口
 * <p>
 * 提供单轮和多轮对话能力。
 * </p>
 * @author Handsome
 */
public interface ChatModel {

    /**
     * 发送消息（非流式）
     *
     * @param userMessage 用户消息
     * @return AI 回复
     */
    Mono<String> chat(String userMessage);

    /**
     * 发送消息（流式）
     *
     * @param userMessage 用户消息
     * @return 流式 AI 回复
     */
    Flux<String> chatStream(String userMessage);

    /**
     * 多轮对话（非流式）
     *
     * @param messages 消息列表（system/user/assistant）
     * @return AI 回复
     */
    Mono<String> chat(List<ChatMessage> messages);

    /**
     * 多轮对话（流式）
     *
     * @param messages 消息列表（system/user/assistant）
     * @return 流式 AI 回复
     */
    Flux<String> chatStream(List<ChatMessage> messages);
}
