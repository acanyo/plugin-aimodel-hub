package com.xhhao.aimodelhub.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * 插件配置获取
 */
@Component
@RequiredArgsConstructor
public class SettingConfigGetter {

    private final ReactiveSettingFetcher settingFetcher;

    /**
     * 获取文字模型配置
     */
    public Mono<TextModelConfig> getTextModelConfig() {
        return settingFetcher.fetch("text", TextModelConfig.class);
    }

    /**
     * 获取图像模型配置
     */
    public Mono<ImageModelConfig> getImageModelConfig() {
        return settingFetcher.fetch("image", ImageModelConfig.class);
    }

    @Data
    public static class TextModelConfig {
        private OpenAiConfig openai;
        private SiliconFlowConfig siliconflow;
        private ZhipuConfig zhipu;
    }

    @Data
    public static class ImageModelConfig {
        private ImageOpenAiConfig openai;
        private ImageZhipuConfig zhipu;
        private ImageSiliconFlowConfig siliconflow;
    }

    @Data
    public static class OpenAiConfig {
        private String baseUrl;
        private String apiKey;
        private String model;
    }

    @Data
    public static class SiliconFlowConfig {
        private String apiKey;
        private String model;
    }

    @Data
    public static class ImageOpenAiConfig {
        private String baseUrl;
        private String apiKey;
        private String model;
        private String size;
    }

    @Data
    public static class ZhipuConfig {
        private String apiKey;
        private String model;
    }

    @Data
    public static class ImageZhipuConfig {
        private String apiKey;
        private String model;
        private String size;
    }

    @Data
    public static class ImageSiliconFlowConfig {
        private String apiKey;
        private String model;
        private String size;
    }
}
