package com.xhhao.aimodelhub.api;

import reactor.core.publisher.Mono;

/**
 * 图像模型工厂接口
 *
 * @author Handsome
 * @since 1.0.0
 */
public interface ImageModelFactory {

    /**
     * 获取 OpenAI DALL-E 图像模型
     */
    Mono<ImageModel> openai();

    /**
     * 获取智谱AI CogView 图像模型
     */
    Mono<ImageModel> zhipu();

    /**
     * 获取硅基流动图像模型 (FLUX/SD)
     */
    Mono<ImageModel> siliconflow();
}
