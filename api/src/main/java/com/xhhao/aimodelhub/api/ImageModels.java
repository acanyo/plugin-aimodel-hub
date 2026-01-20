package com.xhhao.aimodelhub.api;

import com.xhhao.aimodelhub.api.internal.ImageModelFactory;
import com.xhhao.aimodelhub.api.internal.ImageModelsHolder;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 图像生成静态入口
 * <p>
 * 提供最简洁的 API，直接调用 generate 方法生成图像。
 * </p>
 *
 * <pre>{@code
 * // 最简单用法（使用插件设置的默认模型和尺寸）
 * ImageModels.generate("一只可爱的橘猫").subscribe(urls -> {
 *     urls.forEach(System.out::println);
 * });
 *
 * // 自定义选项
 * ImageModels.generate("一只可爱的橘猫", ImageOptions.builder()
 *     .size("1792x1024")
 *     .quality("hd")
 *     .style("vivid")
 *     .build()
 * ).subscribe(urls -> ...);
 * }</pre>
 *
 * @author Handsome
 * @since 1.0.0
 */
public final class ImageModels {

    private static Provider defaultProvider = Provider.SILICONFLOW;

    private ImageModels() {
    }

    private static ImageModelFactory getFactory() {
        return ImageModelsHolder.getFactory();
    }

    /**
     * 生成图像（使用默认供应商和选项）
     *
     * @param prompt 图像描述
     * @return 生成的图像 URL 列表
     */
    public static Mono<List<String>> generate(String prompt) {
        return generate(defaultProvider, prompt, ImageOptions.defaults());
    }

    /**
     * 生成图像（指定供应商）
     *
     * @param provider 供应商
     * @param prompt   图像描述
     * @return 生成的图像 URL 列表
     */
    public static Mono<List<String>> generate(Provider provider, String prompt) {
        return generate(provider, prompt, ImageOptions.defaults());
    }

    /**
     * 生成图像（自定义选项）
     *
     * @param prompt  图像描述
     * @param options 生成选项
     * @return 生成的图像 URL 列表
     */
    public static Mono<List<String>> generate(String prompt, ImageOptions options) {
        return generate(defaultProvider, prompt, options);
    }

    /**
     * 生成图像（指定供应商和选项）
     *
     * @param provider 供应商
     * @param prompt   图像描述
     * @param options  生成选项
     * @return 生成的图像 URL 列表
     */
    public static Mono<List<String>> generate(Provider provider, String prompt, ImageOptions options) {
        checkInitialized();
        return getModel(provider).flatMap(model -> model.generate(prompt, options));
    }

    /**
     * 生成图像（自定义 apiKey 和 model）
     *
     * @param provider 供应商
     * @param apiKey   API Key
     * @param model    模型名称
     * @param prompt   图像描述
     * @return 生成的图像 URL 列表
     */
    public static Mono<List<String>> generate(Provider provider, String apiKey, String model, String prompt) {
        checkInitialized();
        return getFactory().create(provider.name().toLowerCase(), 
                ImageOptions.builder().apiKey(apiKey).model(model).build())
            .flatMap(m -> m.generate(prompt, ImageOptions.defaults()));
    }

    /**
     * 生成图像（使用完整配置）
     *
     * @param provider 供应商
     * @param options  包含 apiKey 的完整配置
     * @param prompt   图像描述
     * @return 生成的图像 URL 列表
     */
    public static Mono<List<String>> generate(Provider provider, ImageOptions options, String prompt) {
        checkInitialized();
        if (options.getApiKey() != null) {
            return getFactory().create(provider.name().toLowerCase(), options)
                .flatMap(m -> m.generate(prompt, options));
        }
        return getModel(provider).flatMap(model -> model.generate(prompt, options));
    }

    /**
     * 设置默认供应商
     */
    public static void setDefaultProvider(Provider provider) {
        defaultProvider = provider;
    }

    /**
     * 获取默认供应商
     */
    public static Provider getDefaultProvider() {
        return defaultProvider;
    }

    private static Mono<ImageModel> getModel(Provider provider) {
        return switch (provider) {
            case OPENAI -> getFactory().openai();
            case ZHIPU -> getFactory().zhipu();
            case SILICONFLOW -> getFactory().siliconflow();
        };
    }

    private static void checkInitialized() {
        if (getFactory() == null) {
            throw new IllegalStateException("ImageModels 未初始化，请确保 AI Model Hub 插件已启动");
        }
    }

    /**
     * 图像模型供应商枚举
     */
    public enum Provider {
        OPENAI,
        ZHIPU,
        SILICONFLOW
    }
}
