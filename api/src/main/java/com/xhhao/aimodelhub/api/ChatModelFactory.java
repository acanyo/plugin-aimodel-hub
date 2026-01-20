package com.xhhao.aimodelhub.api;

import reactor.core.publisher.Mono;

/**
 * 聊天模型工厂接口
 *
 * @author Handsome
 * @since 1.0.0
 */
public interface ChatModelFactory {

    /**
     * 获取 OpenAI 模型（响应式）
     */
    Mono<ChatModel> openai();

    /**
     * 获取硅基流动模型（响应式）
     */
    Mono<ChatModel> siliconflow();

    /**
     * 获取智谱AI模型（响应式）
     */
    Mono<ChatModel> zhipu();

    /**
     * 获取带有记忆的模型（响应式）
     *
     * @param provider     提供商名称(openai/siliconflow)
     * @param systemPrompt 系统提示语
     * @return 带有记忆的 ChatModel
     */
    Mono<ChatModel> withMemory(String provider, String systemPrompt);
}
