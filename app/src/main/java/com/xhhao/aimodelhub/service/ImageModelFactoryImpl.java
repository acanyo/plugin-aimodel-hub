package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ImageModel;
import com.xhhao.aimodelhub.api.ImageOptions;
import com.xhhao.aimodelhub.api.internal.ImageModelFactory;
import com.xhhao.aimodelhub.config.SettingConfigGetter;
import com.xhhao.aimodelhub.service.common.AiChatLogService;
import com.xhhao.aimodelhub.service.common.LoggingImageModel;
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
    private final AiChatLogService logService;
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public Mono<ImageModel> openai() {
        return configGetter.getImageModelConfig()
            .map(config -> {
                var openaiConfig = config.getOpenai();
                if (openaiConfig == null || openaiConfig.getApiKey() == null || openaiConfig.getApiKey().isBlank()) {
                    throw new ServerWebInputException("请先在插件设置中配置图像模型 API Key");
                }
                ImageModel model = new OpenAiImageModel(openaiConfig, webClient);
                String modelName = openaiConfig.getModel() != null ? openaiConfig.getModel() : "dall-e-3";
                return new LoggingImageModel(model, logService, "openai", modelName);
            });
    }

    @Override
    public Mono<ImageModel> zhipu() {
        return configGetter.getImageModelConfig()
            .map(config -> {
                var zhipuConfig = config.getZhipu();
                if (zhipuConfig == null || zhipuConfig.getApiKey() == null || zhipuConfig.getApiKey().isBlank()) {
                    throw new ServerWebInputException("请先在插件设置中配置智谱AI图像模型 API Key");
                }
                ImageModel model = new ZhipuImageModel(zhipuConfig, webClient);
                String modelName = zhipuConfig.getModel() != null ? zhipuConfig.getModel() : "cogview-3-flash";
                return new LoggingImageModel(model, logService, "zhipu", modelName);
            });
    }

    @Override
    public Mono<ImageModel> siliconflow() {
        return configGetter.getImageModelConfig()
            .map(config -> {
                var sfConfig = config.getSiliconflow();
                if (sfConfig == null || sfConfig.getApiKey() == null || sfConfig.getApiKey().isBlank()) {
                    throw new ServerWebInputException("请先在插件设置中配置硅基流动图像模型 API Key");
                }
                ImageModel model = new SiliconFlowImageModel(sfConfig, webClient);
                String modelName = sfConfig.getModel() != null ? sfConfig.getModel() : "FLUX.1-schnell";
                return new LoggingImageModel(model, logService, "siliconflow", modelName);
            });
    }

    @Override
    public Mono<ImageModel> openai(String apiKey, String model) {
        return create("openai", ImageOptions.builder().apiKey(apiKey).model(model).build());
    }

    @Override
    public Mono<ImageModel> zhipu(String apiKey, String model) {
        return create("zhipu", ImageOptions.builder().apiKey(apiKey).model(model).build());
    }

    @Override
    public Mono<ImageModel> siliconflow(String apiKey, String model) {
        return create("siliconflow", ImageOptions.builder().apiKey(apiKey).model(model).build());
    }

    @Override
    public Mono<ImageModel> create(String provider, ImageOptions options) {
        if (options.getApiKey() == null || options.getApiKey().isBlank()) {
            throw new ServerWebInputException("apiKey 不能为空");
        }
        String actualProvider = provider != null ? provider.toLowerCase() : "openai";
        String apiKey = options.getApiKey();
        String model = options.getModel();

        return switch (actualProvider) {
            case "openai" -> {
                String actualModel = model != null ? model : "dall-e-3";
                ImageModel imageModel = new CustomOpenAiImageModel(apiKey, options.getBaseUrl(), actualModel, webClient);
                yield Mono.just(new LoggingImageModel(imageModel, logService, "openai", actualModel));
            }
            case "zhipu" -> {
                String actualModel = model != null ? model : "cogview-3-flash";
                ImageModel imageModel = new CustomZhipuImageModel(apiKey, actualModel, webClient);
                yield Mono.just(new LoggingImageModel(imageModel, logService, "zhipu", actualModel));
            }
            case "siliconflow" -> {
                String actualModel = model != null ? model : "black-forest-labs/FLUX.1-schnell";
                ImageModel imageModel = new CustomSiliconFlowImageModel(apiKey, actualModel, options, webClient);
                yield Mono.just(new LoggingImageModel(imageModel, logService, "siliconflow", actualModel));
            }
            default -> throw new ServerWebInputException("不支持的供应商: " + actualProvider);
        };
    }

    // ==================== 自定义参数的内部实现 ====================

    private static class CustomOpenAiImageModel implements ImageModel {
        private final String apiKey;
        private final String baseUrl;
        private final String model;
        private final WebClient webClient;

        CustomOpenAiImageModel(String apiKey, String baseUrl, String model, WebClient webClient) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
            this.webClient = webClient;
        }

        @Override
        public Mono<List<String>> generate(String prompt) {
            return generate(prompt, ImageOptions.defaults());
        }

        @Override
        public Mono<List<String>> generate(String prompt, ImageOptions options) {
            String apiUrl = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl.replaceAll("/+$", "") + "/v1/images/generations"
                : OPENAI_IMAGE_API;
            String size = options.getSize() != null ? options.getSize() : "1024x1024";
            int n = model.equals("dall-e-3") ? 1 : Math.max(1, Math.min(options.getN(), 10));

            Map<String, Object> request = new java.util.HashMap<>();
            request.put("prompt", prompt);
            request.put("model", model);
            request.put("size", size);
            request.put("n", n);
            request.put("response_format", "url");
            if (options.getQuality() != null) request.put("quality", options.getQuality());
            if (options.getStyle() != null) request.put("style", options.getStyle());

            return webClient.post().uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request).retrieve()
                .bodyToMono(OpenAiImageModel.ImageResponse.class)
                .map(r -> r.data().stream().map(OpenAiImageModel.ImageData::url).toList());
        }
    }

    private static class CustomZhipuImageModel implements ImageModel {
        private static final String ZHIPU_API = "https://open.bigmodel.cn/api/paas/v4/images/generations";
        private final String apiKey;
        private final String model;
        private final WebClient webClient;

        CustomZhipuImageModel(String apiKey, String model, WebClient webClient) {
            this.apiKey = apiKey;
            this.model = model;
            this.webClient = webClient;
        }

        @Override
        public Mono<List<String>> generate(String prompt) {
            return generate(prompt, ImageOptions.defaults());
        }

        @Override
        public Mono<List<String>> generate(String prompt, ImageOptions options) {
            String size = options.getSize() != null ? options.getSize() : "1024x1024";
            Map<String, Object> request = Map.of("prompt", prompt, "model", model, "size", size);
            return webClient.post().uri(ZHIPU_API)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request).retrieve()
                .bodyToMono(ZhipuImageModel.ZhipuImageResponse.class)
                .map(r -> r.data().stream().map(ZhipuImageModel.ZhipuImageData::url).toList());
        }
    }

    private static class CustomSiliconFlowImageModel implements ImageModel {
        private static final String SF_API = "https://api.siliconflow.cn/v1/images/generations";
        private final String apiKey;
        private final String model;
        private final ImageOptions defaultOptions;
        private final WebClient webClient;

        CustomSiliconFlowImageModel(String apiKey, String model, ImageOptions defaultOptions, WebClient webClient) {
            this.apiKey = apiKey;
            this.model = model;
            this.defaultOptions = defaultOptions;
            this.webClient = webClient;
        }

        @Override
        public Mono<List<String>> generate(String prompt) {
            return generate(prompt, defaultOptions != null ? defaultOptions : ImageOptions.defaults());
        }

        @Override
        public Mono<List<String>> generate(String prompt, ImageOptions options) {
            String size = options.getSize() != null ? options.getSize() : "1024x1024";
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("prompt", prompt);
            request.put("model", model);
            request.put("image_size", size);
            if (options.getSteps() != null) request.put("num_inference_steps", options.getSteps());
            if (options.getGuidanceScale() != null) request.put("guidance_scale", options.getGuidanceScale());
            if (options.getSeed() != null) request.put("seed", options.getSeed());
            if (options.getNegativePrompt() != null) request.put("negative_prompt", options.getNegativePrompt());

            return webClient.post().uri(SF_API)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request).retrieve()
                .bodyToMono(SiliconFlowImageModel.SiliconFlowImageResponse.class)
                .map(r -> r.images().stream().map(SiliconFlowImageModel.SiliconFlowImageData::url).toList());
        }
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

    /**
     * 智谱AI CogView 图像模型实现
     */
    private static class ZhipuImageModel implements ImageModel {

        private static final String ZHIPU_IMAGE_API = "https://open.bigmodel.cn/api/paas/v4/images/generations";

        private final SettingConfigGetter.ImageZhipuConfig config;
        private final WebClient webClient;

        ZhipuImageModel(SettingConfigGetter.ImageZhipuConfig config, WebClient webClient) {
            this.config = config;
            this.webClient = webClient;
        }

        @Override
        public Mono<List<String>> generate(String prompt) {
            return generate(prompt, ImageOptions.defaults());
        }

        @Override
        public Mono<List<String>> generate(String prompt, ImageOptions options) {
            String model = config.getModel();
            String size = options.getSize() != null ? options.getSize() : config.getSize();
            String actualModel = (model == null || model.isBlank()) ? "cogview-3-flash" : model;
            String actualSize = (size == null || size.isBlank()) ? "1024x1024" : size;

            Map<String, Object> request = new java.util.HashMap<>();
            request.put("prompt", prompt);
            request.put("model", actualModel);
            request.put("size", actualSize);

            log.info("智谱AI生成图像: prompt={}, model={}, size={}", prompt, actualModel, actualSize);

            return webClient.post()
                .uri(ZHIPU_IMAGE_API)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ZhipuImageResponse.class)
                .map(response -> response.data().stream()
                    .map(ZhipuImageData::url)
                    .toList())
                .doOnSuccess(urls -> log.info("智谱AI图像生成成功: {} 张", urls.size()))
                .doOnError(e -> log.error("智谱AI图像生成失败: {}", e.getMessage()));
        }

        record ZhipuImageResponse(long created, List<ZhipuImageData> data) {}
        record ZhipuImageData(String url) {}
    }

    /**
     * 硅基流动图像模型实现 (FLUX/SD)
     */
    private static class SiliconFlowImageModel implements ImageModel {

        private static final String SILICONFLOW_IMAGE_API = "https://api.siliconflow.cn/v1/images/generations";

        private final SettingConfigGetter.ImageSiliconFlowConfig config;
        private final WebClient webClient;

        SiliconFlowImageModel(SettingConfigGetter.ImageSiliconFlowConfig config, WebClient webClient) {
            this.config = config;
            this.webClient = webClient;
        }

        @Override
        public Mono<List<String>> generate(String prompt) {
            return generate(prompt, ImageOptions.defaults());
        }

        @Override
        public Mono<List<String>> generate(String prompt, ImageOptions options) {
            String model = config.getModel();
            String size = options.getSize() != null ? options.getSize() : config.getSize();
            String actualModel = (model == null || model.isBlank()) ? "black-forest-labs/FLUX.1-schnell" : model;
            String actualSize = (size == null || size.isBlank()) ? "1024x1024" : size;

            Map<String, Object> request = new java.util.HashMap<>();
            request.put("prompt", prompt);
            request.put("model", actualModel);
            request.put("image_size", actualSize);

            log.info("硅基流动生成图像: prompt={}, model={}, size={}", prompt, actualModel, actualSize);

            return webClient.post()
                .uri(SILICONFLOW_IMAGE_API)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SiliconFlowImageResponse.class)
                .map(response -> response.images().stream()
                    .map(SiliconFlowImageData::url)
                    .toList())
                .doOnSuccess(urls -> log.info("硅基流动图像生成成功: {} 张", urls.size()))
                .doOnError(e -> log.error("硅基流动图像生成失败: {}", e.getMessage()));
        }

        record SiliconFlowImageResponse(List<SiliconFlowImageData> images, long seed) {}
        record SiliconFlowImageData(String url) {}
    }
}
