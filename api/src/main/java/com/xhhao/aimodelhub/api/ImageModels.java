package com.xhhao.aimodelhub.api;

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

    private static ImageModelFactory factory;
    private static Provider defaultProvider = Provider.SILICONFLOW;

    private ImageModels() {
    }

    /**
     * 初始化（由插件启动时调用）
     */
    public static void init(ImageModelFactory imageModelFactory) {
        factory = imageModelFactory;
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
            case OPENAI -> factory.openai();
            case ZHIPU -> factory.zhipu();
            case SILICONFLOW -> factory.siliconflow();
        };
    }

    private static void checkInitialized() {
        if (factory == null) {
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
