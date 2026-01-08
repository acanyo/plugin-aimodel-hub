package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.api.ChatModelFactory;
import com.xhhao.aimodelhub.config.SettingConfigGetter;
import com.xhhao.aimodelhub.service.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 聊天模型工厂实现
 *
 * @author Handsome
 */
@Component
@RequiredArgsConstructor
public class ChatModelFactoryImpl implements ChatModelFactory {

    private final SettingConfigGetter configGetter;

    @Override
    public ChatModel openai() {
        return openai(null);
    }

    @Override
    public ChatModel openai(String modelName) {
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

        return builder.build();
    }

    @Override
    public ChatModel openaiWithMemory(String systemPrompt) {
        ChatModel delegate = openai();
        return new StatefulChatModelImpl(delegate, systemPrompt);
    }
}
