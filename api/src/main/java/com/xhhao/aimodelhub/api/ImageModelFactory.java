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
}
