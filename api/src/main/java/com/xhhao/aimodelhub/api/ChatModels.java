package com.xhhao.aimodelhub.api;

import com.xhhao.aimodelhub.api.internal.ChatModelFactory;
import com.xhhao.aimodelhub.api.internal.ChatModelsHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AI 模型静态入口
 * <p>
 * 提供最简洁的 API，直接调用 chat/chatStream 方法。
 * </p>
 *
 * <pre>{@code
 * // 最简单用法（使用默认供应商）
 * ChatModels.chat("你好").subscribe(System.out::println);
 * ChatModels.chatStream("你好").subscribe(System.out::print);
 *
 * // 指定供应商
 * ChatModels.chat(Provider.SILICONFLOW, "你好").subscribe(System.out::println);
 *
 * // 多轮对话
 * List<ChatMessage> messages = List.of(
 *     ChatMessage.system("你是助手"),
 *     ChatMessage.user("你好")
 * );
 * ChatModels.chat(messages).subscribe(System.out::println);
 *
 * // 带记忆的对话（自动管理上下文）
 * ChatModel model = ChatModels.withMemory("你是助手");
 * model.chat("你好");  // 第一轮
 * model.chat("再见");  // 第二轮，自动带上下文
 * }</pre>
 *
 * @author Handsome
 * @since 1.0.0
 */
public final class ChatModels {

    private static Provider defaultProvider = Provider.SILICONFLOW;

    private ChatModels() {
    }

    private static ChatModelFactory getFactory() {
        return ChatModelsHolder.getFactory();
    }


    /**
     * 发送消息（使用默认供应商）
     */
    public static Mono<String> chat(String message) {
        return chat(defaultProvider, message);
    }

    /**
     * 发送消息（指定供应商）
     */
    public static Mono<String> chat(Provider provider, String message) {
        return getModel(provider).flatMap(model -> model.chat(message));
    }

    /**
     * 流式发送消息（使用默认供应商）
     */
    public static Flux<String> chatStream(String message) {
        return chatStream(defaultProvider, message);
    }

    /**
     * 流式发送消息（指定供应商）
     */
    public static Flux<String> chatStream(Provider provider, String message) {
        return getModel(provider).flatMapMany(model -> model.chatStream(message));
    }

    /**
     * 发送消息（自定义 apiKey 和 model）
     */
    public static Mono<String> chat(Provider provider, String apiKey, String model, String message) {
        return getModel(provider, apiKey, model).flatMap(m -> m.chat(message));
    }

    /**
     * 流式发送消息（自定义 apiKey 和 model）
     */
    public static Flux<String> chatStream(Provider provider, String apiKey, String model, String message) {
        return getModel(provider, apiKey, model).flatMapMany(m -> m.chatStream(message));
    }

    /**
     * 发送消息（使用完整配置）
     *
     * <pre>{@code
     * ChatOptions options = ChatOptions.builder()
     *     .apiKey("sk-xxx")
     *     .model("gpt-4o")
     *     .temperature(0.7)
     *     .maxTokens(2048)
     *     .build();
     * ChatModels.chat(Provider.OPENAI, options, "你好");
     * }</pre>
     */
    public static Mono<String> chat(Provider provider, ChatOptions options, String message) {
        checkInitialized();
        return getFactory().create(provider.name().toLowerCase(), options)
            .flatMap(m -> m.chat(message));
    }

    /**
     * 流式发送消息（使用完整配置）
     */
    public static Flux<String> chatStream(Provider provider, ChatOptions options, String message) {
        checkInitialized();
        return getFactory().create(provider.name().toLowerCase(), options)
            .flatMapMany(m -> m.chatStream(message));
    }

    /**
     * 多轮对话（使用完整配置）
     */
    public static Mono<String> chat(Provider provider, ChatOptions options, List<ChatMessage> messages) {
        checkInitialized();
        return getFactory().create(provider.name().toLowerCase(), options)
            .flatMap(m -> m.chat(messages));
    }

    /**
     * 多轮对话流式（使用完整配置）
     */
    public static Flux<String> chatStream(Provider provider, ChatOptions options, List<ChatMessage> messages) {
        checkInitialized();
        return getFactory().create(provider.name().toLowerCase(), options)
            .flatMapMany(m -> m.chatStream(messages));
    }

    /**
     * 多轮对话（使用默认供应商）
     */
    public static Mono<String> chat(List<ChatMessage> messages) {
        return chat(defaultProvider, messages);
    }

    /**
     * 多轮对话（指定供应商）
     */
    public static Mono<String> chat(Provider provider, List<ChatMessage> messages) {
        return getModel(provider).flatMap(model -> model.chat(messages));
    }

    /**
     * 多轮对话流式（使用默认供应商）
     */
    public static Flux<String> chatStream(List<ChatMessage> messages) {
        return chatStream(defaultProvider, messages);
    }

    /**
     * 多轮对话流式（指定供应商）
     */
    public static Flux<String> chatStream(Provider provider, List<ChatMessage> messages) {
        return getModel(provider).flatMapMany(model -> model.chatStream(messages));
    }

    /**
     * 获取带记忆的模型（使用默认供应商）
     * <p>
     * 返回的 ChatModel 会自动维护对话历史
     * </p>
     *
     * @param systemPrompt 系统提示词（角色设定）
     * @return 带记忆的 ChatModel
     */
    public static Mono<ChatModel> withMemory(String systemPrompt) {
        return withMemory(defaultProvider, systemPrompt);
    }

    /**
     * 获取带记忆的模型（指定供应商）
     *
     * @param provider     供应商
     * @param systemPrompt 系统提示词
     * @return 带记忆的 ChatModel
     */
    public static Mono<ChatModel> withMemory(Provider provider, String systemPrompt) {
        checkInitialized();
        return getFactory().withMemory(provider.name().toLowerCase(), systemPrompt);
    }

    /**
     * 设置默认供应商
     */
    public static void setDefaultProvider(Provider provider) {
        defaultProvider = provider;
    }

    /**
     * 获取默认供应商
     */
    public static Provider getDefaultProvider() {
        return defaultProvider;
    }

    private static Mono<ChatModel> getModel(Provider provider) {
        checkInitialized();
        return switch (provider) {
            case OPENAI -> getFactory().openai();
            case SILICONFLOW -> getFactory().siliconflow();
            case ZHIPU -> getFactory().zhipu();
        };
    }

    private static Mono<ChatModel> getModel(Provider provider, String apiKey, String model) {
        checkInitialized();
        return switch (provider) {
            case OPENAI -> getFactory().openai(apiKey, model);
            case SILICONFLOW -> getFactory().siliconflow(apiKey, model);
            case ZHIPU -> getFactory().zhipu(apiKey, model);
        };
    }

    private static void checkInitialized() {
        if (getFactory() == null) {
            throw new IllegalStateException("ChatModels 未初始化，请确保 AI Model Hub 插件已启动");
        }
    }

    /**
     * 模型供应商枚举
     */
    public enum Provider {
        OPENAI,
        SILICONFLOW,
        ZHIPU
    }
}
