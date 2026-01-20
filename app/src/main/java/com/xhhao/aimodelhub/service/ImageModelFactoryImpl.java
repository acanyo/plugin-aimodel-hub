package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ImageModel;
import com.xhhao.aimodelhub.api.ImageModelFactory;
import com.xhhao.aimodelhub.api.ImageOptions;
import com.xhhao.aimodelhub.config.SettingConfigGetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 图像模型工厂实现
 *
 * @author Handsome
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageModelFactoryImpl implements ImageModelFactory {

    private static final String OPENAI_IMAGE_API = "https://api.openai.com/v1/images/generations";

    private final SettingConfigGetter configGetter;
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public Mono<ImageModel> openai() {
        return configGetter.getImageModelConfig()
            .map(config -> {
                var openaiConfig = config.getOpenai();
                if (openaiConfig == null || openaiConfig.getApiKey() == null || openaiConfig.getApiKey().isBlank()) {
                    throw new ServerWebInputException("请先在插件设置中配置图像模型 API Key");
                }
                return new OpenAiImageModel(openaiConfig, webClient);
            });
    }

    /**
     * OpenAI DALL-E 图像模型实现
     */
    private static class OpenAiImageModel implements ImageModel {

        private final SettingConfigGetter.ImageOpenAiConfig config;
        private final WebClient webClient;

        OpenAiImageModel(SettingConfigGetter.ImageOpenAiConfig config, WebClient webClient) {
            this.config = config;
            this.webClient = webClient;
        }

        @Override
        public Mono<List<String>> generate(String prompt) {
            return generate(prompt, ImageOptions.defaults());
        }

        @Override
        public Mono<List<String>> generate(String prompt, ImageOptions options) {
            String baseUrl = config.getBaseUrl();
            String apiUrl = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl.replaceAll("/+$", "") + "/v1/images/generations"
                : OPENAI_IMAGE_API;

            String model = config.getModel();
            String size = options.getSize() != null ? options.getSize() : config.getSize();
            String actualModel = (model == null || model.isBlank()) ? "dall-e-3" : model;
            String actualSize = (size == null || size.isBlank()) ? "1024x1024" : size;
            int n = actualModel.equals("dall-e-3") ? 1 : Math.max(1, Math.min(options.getN(), 10));

            Map<String, Object> request = new java.util.HashMap<>();
            request.put("prompt", prompt);
            request.put("model", actualModel);
            request.put("size", actualSize);
            request.put("n", n);
            request.put("response_format", "url");

            if (options.getQuality() != null) {
                request.put("quality", options.getQuality());
            }
            if (options.getStyle() != null) {
                request.put("style", options.getStyle());
            }

            log.info("生成图像: prompt={}, model={}, size={}", prompt, actualModel, actualSize);

            return webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ImageResponse.class)
                .map(response -> response.data().stream()
                    .map(ImageData::url)
                    .toList())
                .doOnSuccess(urls -> log.info("图像生成成功: {} 张", urls.size()))
                .doOnError(e -> log.error("图像生成失败: {}", e.getMessage()));
        }

        record ImageResponse(long created, List<ImageData> data) {}
        record ImageData(String url, String revisedPrompt, String b64Json) {}
    }
}
