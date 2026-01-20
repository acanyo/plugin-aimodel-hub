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

    private ImageModels() {
    }

    /**
     * 初始化（由插件启动时调用）
     */
    public static void init(ImageModelFactory imageModelFactory) {
        factory = imageModelFactory;
    }

    /**
     * 生成图像（使用默认选项）
     *
     * @param prompt 图像描述
     * @return 生成的图像 URL 列表
     */
    public static Mono<List<String>> generate(String prompt) {
        return generate(prompt, ImageOptions.defaults());
    }

    /**
     * 生成图像（自定义选项）
     *
     * @param prompt  图像描述
     * @param options 生成选项
     * @return 生成的图像 URL 列表
     */
    public static Mono<List<String>> generate(String prompt, ImageOptions options) {
        checkInitialized();
        return factory.openai().flatMap(model -> model.generate(prompt, options));
    }

    private static void checkInitialized() {
        if (factory == null) {
            throw new IllegalStateException("ImageModels 未初始化，请确保 AI Model Hub 插件已启动");
        }
    }
}
