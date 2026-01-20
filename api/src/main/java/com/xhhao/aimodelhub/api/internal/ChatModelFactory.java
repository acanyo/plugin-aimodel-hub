package com.xhhao.aimodelhub.api.internal;

import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.api.ChatOptions;
import reactor.core.publisher.Mono;

/**
 * 聊天模型工厂接口（内部使用）
 * <p>
 * 此接口仅供插件内部实现使用，外部插件请使用 {@link com.xhhao.aimodelhub.api.ChatModels} 静态方法。
 * </p>
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
     * 获取 OpenAI 模型（指定模型名称）
     */
    Mono<ChatModel> openai(String modelName);

    /**
     * 获取硅基流动模型（响应式）
     */
    Mono<ChatModel> siliconflow();

    /**
     * 获取硅基流动模型（指定模型名称）
     */
    Mono<ChatModel> siliconflow(String modelName);

    /**
     * 获取智谱AI模型（响应式）
     */
    Mono<ChatModel> zhipu();

    /**
     * 获取智谱AI模型（指定模型名称）
     */
    Mono<ChatModel> zhipu(String modelName);

    /**
     * 获取带有记忆的模型（响应式）
     *
     * @param provider     提供商名称(openai/siliconflow)
     * @param systemPrompt 系统提示语
     * @return 带有记忆的 ChatModel
     */
    Mono<ChatModel> withMemory(String provider, String systemPrompt);

    /**
     * 获取 OpenAI 模型（使用自定义 apiKey 和 model）
     */
    Mono<ChatModel> openai(String apiKey, String model);

    /**
     * 获取硅基流动模型（使用自定义 apiKey 和 model）
     */
    Mono<ChatModel> siliconflow(String apiKey, String model);

    /**
     * 获取智谱AI模型（使用自定义 apiKey 和 model）
     */
    Mono<ChatModel> zhipu(String apiKey, String model);

    /**
     * 使用完整配置创建模型
     *
     * @param provider 供应商名称（openai/siliconflow/zhipu）
     * @param options  调用参数
     * @return ChatModel
     */
    Mono<ChatModel> create(String provider, ChatOptions options);
}
