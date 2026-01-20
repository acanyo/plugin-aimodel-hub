package com.xhhao.aimodelhub.service.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.xhhao.aimodelhub.api.ChatMessage;
import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.api.constant.AiModelConstants;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 API 客户端
 * <p>
 * 支持所有使用 OpenAI 兼容接口的服务商（OpenAI、硅基流动等）
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
@Slf4j
@Getter
public class OpenAiCompatibleChatModel implements ChatModel {

    private static final String DEFAULT_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String SSE_DONE_SIGNAL = "[DONE]";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE_REF =
        new ParameterizedTypeReference<>() {};

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final Map<String, String> customHeaders;
    private final String organizationId;
    private final String projectId;
    private final Duration timeout;
    private final Integer maxRetries;

    private final Double temperature;
    private final Double topP;
    private final Integer maxTokens;
    private final Integer maxCompletionTokens;
    private final Double frequencyPenalty;
    private final Double presencePenalty;
    private final List<String> stop;
    private final Integer seed;
    private final String user;
    private final Map<String, Integer> logitBias;
    private final String chatCompletionsPath;

    /**
     * 缓存的 WebClient 实例
     */
    private final WebClient webClient;

    @Builder
    public OpenAiCompatibleChatModel(String apiKey, String modelName, String baseUrl,
                                     Map<String, String> customHeaders, String organizationId, String projectId,
                                     Duration timeout, Integer maxRetries,
                                     Double temperature, Double topP, Integer maxTokens, Integer maxCompletionTokens,
                                     Double frequencyPenalty, Double presencePenalty, List<String> stop,
                                     Integer seed, String user, Map<String, Integer> logitBias,
                                     String chatCompletionsPath) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.chatCompletionsPath = chatCompletionsPath != null ? chatCompletionsPath : DEFAULT_CHAT_COMPLETIONS_PATH;
        this.customHeaders = customHeaders;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(AiModelConstants.DEFAULT_TIMEOUT_SECONDS);
        this.maxRetries = maxRetries != null ? maxRetries : AiModelConstants.DEFAULT_MAX_RETRIES;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.maxCompletionTokens = maxCompletionTokens;
        this.frequencyPenalty = frequencyPenalty;
        this.presencePenalty = presencePenalty;
        this.stop = stop;
        this.seed = seed;
        this.user = user;
        this.logitBias = logitBias;
        this.webClient = createWebClient();
    }

    /**
     * 发送聊天请求（非流式）
     */
    public Mono<OpenAiChatResponse> chat(OpenAiChatRequest request) {
        prepareRequest(request, false);
        return webClient.post()
            .uri(chatCompletionsPath)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OpenAiChatResponse.class);
    }

    /**
     * 发送流式聊天请求
     */
    public Flux<OpenAiChatResponse> chatStream(OpenAiChatRequest request) {
        prepareRequest(request, true);
        return webClient.post()
            .uri(chatCompletionsPath)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(SSE_TYPE_REF)
            .filter(sse -> sse.data() != null && !SSE_DONE_SIGNAL.equals(sse.data()))
            .map(ServerSentEvent::data)
            .mapNotNull(this::parseResponse);
    }

    @Override
    public Mono<String> chat(String userMessage) {
        return chat(List.of(ChatMessage.user(userMessage)));
    }

    @Override
    public Flux<String> chatStream(String userMessage) {
        return chatStream(List.of(ChatMessage.user(userMessage)));
    }

    @Override
    public Mono<String> chat(List<ChatMessage> messages) {
        OpenAiChatRequest request = buildRequest(messages);
        return chat(request).map(OpenAiChatResponse::getContent);
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages) {
        OpenAiChatRequest request = buildRequest(messages);
        return chatStream(request)
            .filter(response -> response.getContent() != null && !response.getContent().isEmpty())
            .map(OpenAiChatResponse::getContent);
    }

    /**
     * 构建请求对象
     */
    private OpenAiChatRequest buildRequest(List<ChatMessage> messages) {
        List<OpenAiMessage> openAiMessages = messages.stream()
            .map(msg -> new OpenAiMessage(msg.getRole(), msg.getContent(), null, null, null))
            .toList();
        return OpenAiChatRequest.builder()
            .model(modelName)
            .messages(openAiMessages)
            .build();
    }

    /**
     * 准备请求：设置默认值
     */
    private void prepareRequest(OpenAiChatRequest request, boolean stream) {
        if (request.getModel() == null) {
            request.setModel(modelName);
        }
        if (stream) {
            request.setStream(true);
        }
        applyDefaults(request);
    }

    /**
     * 创建 WebClient 实例
     */
    private WebClient createWebClient() {
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(AUTH_HEADER, BEARER_PREFIX + apiKey)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        if (organizationId != null) {
            builder.defaultHeader("OpenAI-Organization", organizationId);
        }
        if (projectId != null) {
            builder.defaultHeader("OpenAI-Project", projectId);
        }
        if (customHeaders != null) {
            customHeaders.forEach(builder::defaultHeader);
        }

        return builder.build();
    }

    /**
     * 应用默认参数
     */
    private void applyDefaults(OpenAiChatRequest request) {
        setIfNull(request::getTemperature, request::setTemperature, temperature);
        setIfNull(request::getTopP, request::setTopP, topP);
        setIfNull(request::getMaxTokens, request::setMaxTokens, maxTokens);
        setIfNull(request::getMaxCompletionTokens, request::setMaxCompletionTokens, maxCompletionTokens);
        setIfNull(request::getFrequencyPenalty, request::setFrequencyPenalty, frequencyPenalty);
        setIfNull(request::getPresencePenalty, request::setPresencePenalty, presencePenalty);
        setIfNull(request::getStop, request::setStop, stop);
        setIfNull(request::getSeed, request::setSeed, seed);
        setIfNull(request::getUser, request::setUser, user);
        setIfNull(request::getLogitBias, request::setLogitBias, logitBias);
    }

    /**
     * 如果请求值为 null 且默认值不为 null，则设置默认值
     */
    private <T> void setIfNull(java.util.function.Supplier<T> getter,
                               java.util.function.Consumer<T> setter,
                               T defaultValue) {
        if (getter.get() == null && defaultValue != null) {
            setter.accept(defaultValue);
        }
    }

    /**
     * 解析 SSE 响应 JSON
     */
    private OpenAiChatResponse parseResponse(String json) {
        try {
            return MAPPER.readValue(json, OpenAiChatResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("解析响应失败: {}", json, e);
            return null;
        }
    }
}
