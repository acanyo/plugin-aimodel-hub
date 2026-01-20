package com.xhhao.aimodelhub.api.internal;

import com.xhhao.aimodelhub.api.ImageModel;
import com.xhhao.aimodelhub.api.ImageOptions;
import reactor.core.publisher.Mono;

/**
 * 图像模型工厂接口（内部使用）
 * <p>
 * 此接口仅供插件内部实现使用，外部插件请使用 {@link com.xhhao.aimodelhub.api.ImageModels} 静态方法。
 * </p>
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

    /**
     * 获取 OpenAI 图像模型（自定义 apiKey 和 model）
     */
    Mono<ImageModel> openai(String apiKey, String model);

    /**
     * 获取智谱图像模型（自定义 apiKey 和 model）
     */
    Mono<ImageModel> zhipu(String apiKey, String model);

    /**
     * 获取硅基流动图像模型（自定义 apiKey 和 model）
     */
    Mono<ImageModel> siliconflow(String apiKey, String model);

    /**
     * 使用完整配置创建图像模型
     */
    Mono<ImageModel> create(String provider, ImageOptions options);
}
