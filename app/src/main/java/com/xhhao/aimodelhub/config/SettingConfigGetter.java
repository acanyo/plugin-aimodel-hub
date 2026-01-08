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

    public Mono<ProviderConfig> getProviderConfig() {
        return settingFetcher.fetch("provider", ProviderConfig.class);
    }

    @Data
    public static class ProviderConfig {
        private OpenAiConfig openai;
    }

    @Data
    public static class OpenAiConfig {
        private String baseUrl;
        private String apiKey;
        private String model;
    }
}
