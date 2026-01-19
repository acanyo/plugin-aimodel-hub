package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.api.ChatModelFactory;
import com.xhhao.aimodelhub.api.constant.AiModelConstants;
import com.xhhao.aimodelhub.api.exception.AiModelException;
import com.xhhao.aimodelhub.config.SettingConfigGetter;
import com.xhhao.aimodelhub.config.SettingConfigGetter.OpenAiConfig;
import com.xhhao.aimodelhub.service.openai.OpenAiChatModel;
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
    public ChatModel openai() {
        return openaiReactive(null, null).block();
    }

    @Override
    public ChatModel openai(String modelName) {
        return openaiReactive(modelName, null).block();
    }

    /**
     * 获取 OpenAI 模型（响应式版本）
     * <p>
     * 推荐使用此方法，避免阻塞调用
     * </p>
     *
     * @param modelName    模型名称，为 null 则使用配置的默认模型
     * @param callerPlugin 调用者插件名称（用于日志记录）
     * @return 带日志记录的 ChatModel
     */
    public Mono<ChatModel> openaiReactive(String modelName, String callerPlugin) {
        return configGetter.getProviderConfig()
            .flatMap(config -> {
                if (config == null || config.getOpenai() == null) {
                    return Mono.error(AiModelException.configError("OpenAI 未配置，请在插件设置中配置 API Key"));
                }
                return Mono.just(createOpenAiModel(config.getOpenai(), modelName, callerPlugin));
            })
            .doOnError(e -> log.error("创建 OpenAI 模型失败", e));
    }

    /**
     * 创建 OpenAI 模型实例
     *
     * @param openaiConfig OpenAI 配置
     * @param modelName    模型名称
     * @param callerPlugin 调用者插件
     * @return 包装后的 ChatModel
     */
    private ChatModel createOpenAiModel(OpenAiConfig openaiConfig, String modelName, String callerPlugin) {
        String actualModel = resolveModelName(modelName, openaiConfig.getModel());
        
        OpenAiChatModel delegate = OpenAiChatModel.builder()
            .apiKey(openaiConfig.getApiKey())
            .baseUrl(openaiConfig.getBaseUrl())
            .modelName(actualModel)
            .build();

        return new LoggingChatModel(delegate, logService, callerPlugin, AiModelConstants.Provider.OPENAI);
    }

    /**
     * 解析模型名称
     *
     * @param requestedModel 请求的模型名
     * @param defaultModel   默认模型名
     * @return 实际使用的模型名
     */
    private String resolveModelName(String requestedModel, String defaultModel) {
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel;
        }
        if (defaultModel != null && !defaultModel.isBlank()) {
            return defaultModel;
        }
        return AiModelConstants.DEFAULT_OPENAI_MODEL;
    }

    @Override
    public ChatModel openaiWithMemory(String systemPrompt) {
        return openaiWithMemoryReactive(systemPrompt, null).block();
    }

    /**
     * 获取带记忆的 OpenAI 模型（响应式版本）
     *
     * @param systemPrompt 系统提示词
     * @param callerPlugin 调用者插件
     * @return 带记忆功能的 ChatModel
     */
    public Mono<ChatModel> openaiWithMemoryReactive(String systemPrompt, String callerPlugin) {
        return openaiReactive(null, callerPlugin)
            .map(delegate -> new StatefulChatModelImpl(delegate, systemPrompt));
    }
}
