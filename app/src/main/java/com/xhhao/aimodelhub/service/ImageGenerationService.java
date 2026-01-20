package com.xhhao.aimodelhub.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xhhao.aimodelhub.config.SettingConfigGetter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 图像生成服务
 * <p>
 * 支持 OpenAI DALL-E 图像生成
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private static final String OPENAI_IMAGE_API = "https://api.openai.com/v1/images/generations";

    private final SettingConfigGetter configGetter;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 生成图像（使用插件配置的默认模型和尺寸）
     *
     * @param prompt 图像描述
     * @return 生成的图像 URL 列表
     */
    public Mono<List<String>> generateImage(String prompt) {
        return configGetter.getImageModelConfig()
            .flatMap(config -> {
                var openaiConfig = config.getOpenai();
                if (openaiConfig == null || openaiConfig.getApiKey() == null || openaiConfig.getApiKey().isBlank()) {
                    return Mono.error(new ServerWebInputException("请先在插件设置中配置图像模型 API Key"));
                }

                String baseUrl = openaiConfig.getBaseUrl();
                String apiUrl = (baseUrl != null && !baseUrl.isBlank()) 
                    ? baseUrl.replaceAll("/+$", "") + "/v1/images/generations"
                    : OPENAI_IMAGE_API;

                String model = openaiConfig.getModel();
                String size = openaiConfig.getSize();
                String actualModel = (model == null || model.isBlank()) ? "dall-e-3" : model;
                String actualSize = (size == null || size.isBlank()) ? "1024x1024" : size;
                int n = actualModel.equals("dall-e-3") ? 1 : 1;

                ImageRequest request = new ImageRequest();
                request.setPrompt(prompt);
                request.setModel(actualModel);
                request.setSize(actualSize);
                request.setN(n);

                log.info("生成图像: prompt={}, model={}, size={}", prompt, actualModel, actualSize);

                return webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + openaiConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ImageResponse.class)
                    .map(response -> response.getData().stream()
                        .map(ImageData::getUrl)
                        .toList())
                    .doOnSuccess(urls -> log.info("图像生成成功: {} 张", urls.size()))
                    .doOnError(e -> log.error("图像生成失败: {}", e.getMessage()));
            });
    }

    @Data
    public static class ImageRequest {
        private String prompt;
        private String model;
        private String size;
        private int n;
        private String quality = "standard";
        @JsonProperty("response_format")
        private String responseFormat = "url";
    }

    @Data
    public static class ImageResponse {
        private long created;
        private List<ImageData> data;
    }

    @Data
    public static class ImageData {
        private String url;
        @JsonProperty("revised_prompt")
        private String revisedPrompt;
        @JsonProperty("b64_json")
        private String b64Json;
    }
}
