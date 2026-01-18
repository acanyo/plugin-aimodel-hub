package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.api.ChatModelFactory;
import com.xhhao.aimodelhub.config.SettingConfigGetter;
import com.xhhao.aimodelhub.service.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 聊天模型工厂实现
 *
 * @author Handsome
 */
@Component
@RequiredArgsConstructor
public class ChatModelFactoryImpl implements ChatModelFactory {

    private final SettingConfigGetter configGetter;
    private final AiChatLogService logService;

    @Override
    public ChatModel openai() {
        return openai(null, null);
    }

    @Override
    public ChatModel openai(String modelName) {
        return openai(modelName, null);
    }

    /**
     * 获取 OpenAI 模型（带调用者标识）- 响应式版本
     *
     * @param modelName    模型名称
     * @param callerPlugin 调用者插件名称（用于日志记录）
     */
    public Mono<ChatModel> openaiReactive(String modelName, String callerPlugin) {
        return configGetter.getProviderConfig()
            .map(config -> {
                if (config == null || config.getOpenai() == null) {
                    throw new IllegalStateException("OpenAI 未配置");
                }

                var openaiConfig = config.getOpenai();
                var builder = OpenAiChatModel.builder()
                    .apiKey(openaiConfig.getApiKey())
                    .baseUrl(openaiConfig.getBaseUrl());

                if (modelName != null && !modelName.isBlank()) {
                    builder.modelName(modelName);
                } else {
                    builder.modelName(openaiConfig.getModel());
                }

                OpenAiChatModel delegate = builder.build();
                
                // 包装为带日志记录的模型
                return (ChatModel) new LoggingChatModel(delegate, logService, callerPlugin, "openai");
            });
    }

    /**
     * 获取 OpenAI 模型（带调用者标识）
     *
     * @param modelName    模型名称
     * @param callerPlugin 调用者插件名称（用于日志记录）
     */
    public ChatModel openai(String modelName, String callerPlugin) {
        var config = configGetter.getProviderConfig().block();
        if (config == null || config.getOpenai() == null) {
            throw new IllegalStateException("OpenAI 未配置");
        }

        var openaiConfig = config.getOpenai();
        var builder = OpenAiChatModel.builder()
            .apiKey(openaiConfig.getApiKey())
            .baseUrl(openaiConfig.getBaseUrl());

        if (modelName != null && !modelName.isBlank()) {
            builder.modelName(modelName);
        } else {
            builder.modelName(openaiConfig.getModel());
        }

        OpenAiChatModel delegate = builder.build();
        
        // 包装为带日志记录的模型
        return new LoggingChatModel(delegate, logService, callerPlugin, "openai");
    }

    @Override
    public ChatModel openaiWithMemory(String systemPrompt) {
        return openaiWithMemory(systemPrompt, null);
    }

    /**
     * 获取带记忆的 OpenAI 模型（带调用者标识）
     */
    public ChatModel openaiWithMemory(String systemPrompt, String callerPlugin) {
        ChatModel delegate = openai(null, callerPlugin);
        return new StatefulChatModelImpl(delegate, systemPrompt);
    }
}
