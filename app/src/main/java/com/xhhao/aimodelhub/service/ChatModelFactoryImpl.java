package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.api.ChatModelFactory;
import com.xhhao.aimodelhub.api.constant.AiModelConstants;
import com.xhhao.aimodelhub.api.exception.AiModelException;
import com.xhhao.aimodelhub.config.SettingConfigGetter;
import com.xhhao.aimodelhub.service.common.AiChatLogService;
import com.xhhao.aimodelhub.service.common.LoggingChatModel;
import com.xhhao.aimodelhub.service.common.StatefulChatModelImpl;
import com.xhhao.aimodelhub.service.openai.OpenAiCompatibleChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 聊天模型工厂实现
 * <p>
 * 负责创建各类 AI 模型客户端，支持 OpenAI 系列模型
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelFactoryImpl implements ChatModelFactory {

    private final SettingConfigGetter configGetter;
    private final AiChatLogService logService;

    @Override
    public Mono<ChatModel> openai() {
        return createOpenAiModel();
    }

    @Override
    public Mono<ChatModel> siliconflow() {
        return createSiliconFlowModel();
    }

    @Override
    public Mono<ChatModel> zhipu() {
        return createZhipuModel();
    }

    private Mono<ChatModel> createOpenAiModel() {
        return configGetter.getTextModelConfig()
            .flatMap(config -> {
                if (config == null || config.getOpenai() == null) {
                    return Mono.error(AiModelException.configError("OpenAI 未配置，请在插件设置中配置 API Key"));
                }
                var openaiConfig = config.getOpenai();
                String model = openaiConfig.getModel();
                if (model == null || model.isBlank()) {
                    model = AiModelConstants.DEFAULT_OPENAI_MODEL;
                }
                OpenAiCompatibleChatModel delegate = OpenAiCompatibleChatModel.builder()
                    .apiKey(openaiConfig.getApiKey())
                    .baseUrl(openaiConfig.getBaseUrl())
                    .modelName(model)
                    .build();
                return Mono.just((ChatModel) new LoggingChatModel(delegate, logService, null, AiModelConstants.Provider.OPENAI));
            })
            .doOnError(e -> log.error("创建 OpenAI 模型失败", e));
    }

    private Mono<ChatModel> createSiliconFlowModel() {
        return configGetter.getTextModelConfig()
            .flatMap(config -> {
                if (config == null || config.getSiliconflow() == null) {
                    return Mono.error(AiModelException.configError("硅基流动未配置，请在插件设置中配置 API Key"));
                }
                var sfConfig = config.getSiliconflow();
                String model = sfConfig.getModel();
                if (model == null || model.isBlank()) {
                    model = AiModelConstants.DEFAULT_SILICONFLOW_MODEL;
                }
                OpenAiCompatibleChatModel delegate = OpenAiCompatibleChatModel.builder()
                    .apiKey(sfConfig.getApiKey())
                    .baseUrl(AiModelConstants.SILICONFLOW_BASE_URL)
                    .modelName(model)
                    .build();
                return Mono.just((ChatModel) new LoggingChatModel(delegate, logService, null, AiModelConstants.Provider.SILICONFLOW));
            })
            .doOnError(e -> log.error("创建硅基流动模型失败", e));
    }

    private Mono<ChatModel> createZhipuModel() {
        return configGetter.getTextModelConfig()
            .flatMap(config -> {
                if (config == null || config.getZhipu() == null) {
                    return Mono.error(AiModelException.configError("智谱AI未配置，请在插件设置中配置 API Key"));
                }
                var zhipuConfig = config.getZhipu();
                String model = zhipuConfig.getModel();
                if (model == null || model.isBlank()) {
                    model = "glm-4-flash";
                }
                OpenAiCompatibleChatModel delegate = OpenAiCompatibleChatModel.builder()
                    .apiKey(zhipuConfig.getApiKey())
                    .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                    .modelName(model)
                    .chatCompletionsPath("/chat/completions")
                    .build();
                return Mono.just((ChatModel) new LoggingChatModel(delegate, logService, null, AiModelConstants.Provider.ZHIPU));
            })
            .doOnError(e -> log.error("创建智谱AI模型失败", e));
    }

    @Override
    public Mono<ChatModel> withMemory(String provider, String systemPrompt) {
        Mono<ChatModel> delegateMono = switch (provider) {
            case "openai" -> openai();
            case "siliconflow" -> siliconflow();
            case "zhipu" -> zhipu();
            default -> Mono.error(new IllegalArgumentException("不支持的供应商: " + provider));
        };
        return delegateMono.map(delegate -> new StatefulChatModelImpl(delegate, systemPrompt));
    }
}
