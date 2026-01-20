package com.xhhao.aimodelhub.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xhhao.aimodelhub.api.constant.AiModelConstants;
import com.xhhao.aimodelhub.config.SettingConfigGetter;
import com.xhhao.aimodelhub.model.ModelListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 模型列表服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService {

    private static final String SILICONFLOW_MODELS_API = "https://api.siliconflow.cn/v1/models";

    private final SettingConfigGetter configGetter;
    private final WebClient webClient = WebClient.builder().build();

    private final Cache<String, List<ModelListItem>> modelCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build();

    /**
     * 获取硅基流动模型列表
     */
    public Mono<List<ModelListItem>> listSiliconFlowModels() {
        List<ModelListItem> cached = modelCache.getIfPresent(AiModelConstants.Provider.SILICONFLOW);
        if (Objects.nonNull(cached)) {
            return Mono.just(cached);
        }

        return Mono.fromCallable(() -> configGetter.getTextModelConfig().block())
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(config -> {
                var sfConfig = config.getSiliconflow();
                if (sfConfig == null || sfConfig.getApiKey() == null || sfConfig.getApiKey().isBlank()) {
                    return Mono.error(new ServerWebInputException("请先配置硅基流动 API Key"));
                }

                return webClient.get()
                    .uri(SILICONFLOW_MODELS_API)
                    .header("Authorization", "Bearer " + sfConfig.getApiKey())
                    .retrieve()
                    .bodyToMono(ModelResponse.class)
                    .map(response -> {
                        List<ModelListItem> items = response.data().stream()
                            .map(m -> new ModelListItem(m.id(), m.id(), m.created()))
                            .toList();
                        modelCache.put(AiModelConstants.Provider.SILICONFLOW, items);
                        return items;
                    });
            })
            .onErrorResume(e -> {
                log.warn("获取硅基流动模型列表失败: {}", e.getMessage());
                return Mono.error(e);
            });
    }

    record ModelResponse(String object, List<ModelData> data) {}

    record ModelData(
        String id,
        String object,
        int created,
        @JsonProperty("owned_by") String ownedBy
    ) {}
}
