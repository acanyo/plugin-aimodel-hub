package com.xhhao.aimodelhub.service.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.xhhao.aimodelhub.api.ChatModel;
import lombok.Builder;
import lombok.Getter;
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
 * OpenAI Chat 模型客户端
 */
@Getter
@Builder
public class OpenAiChatModel implements ChatModel {

    // ===== 从插件配置获取 允许覆盖 =====
    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    
    private final Map<String, String> customHeaders;
    private final String organizationId;
    private final String projectId;
    
    @Builder.Default
    private final Duration timeout = Duration.ofSeconds(120);
    
    @Builder.Default
    private final Integer maxRetries = 3;
    
    @Builder.Default
    private final Boolean logRequests = false;
    
    @Builder.Default
    private final Boolean logResponses = false;

    // ===== 请求默认参数 =====
    private final Double temperature;
    private final Double topP;
    private final Integer maxTokens;
    private final Integer maxCompletionTokens;
    private final Double frequencyPenalty;
    private final Double presencePenalty;
    private final List<String> stop;
    private final Integer seed;
    private final String user;
    private final String responseFormat;
    private final Map<String, Integer> logitBias;
    private final Boolean logprobs;
    private final Integer topLogprobs;
    private final String reasoningEffort;
    private final String serviceTier;
    private final Boolean store;
    private final Map<String, String> metadata;
    private final Boolean parallelToolCalls;
    private final Object toolChoice;
    private final Boolean strictTools;
    private final Boolean strictSchema;

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    /**
     * 发送聊天请求
     */
    public Mono<OpenAiChatResponse> chat(OpenAiChatRequest request) {
        if (request.getModel() == null) {
            request.setModel(modelName);
        }
        applyDefaults(request);

        return createWebClient()
            .post()
            .uri("/v1/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OpenAiChatResponse.class);
    }

    /**
     * 发送流式聊天请求
     */
    public Flux<OpenAiChatResponse> chatStream(OpenAiChatRequest request) {
        if (request.getModel() == null) {
            request.setModel(modelName);
        }
        request.setStream(true);
        applyDefaults(request);

        ParameterizedTypeReference<ServerSentEvent<String>> typeRef =
            new ParameterizedTypeReference<>() {};

        return createWebClient()
            .post()
            .uri("/v1/chat/completions")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(typeRef)
            .filter(sse -> sse.data() != null && !sse.data().equals("[DONE]"))
            .map(ServerSentEvent::data)
            .mapNotNull(this::parseResponse);
    }

    /**
     * 单轮对话
     */
    @Override
    public Mono<String> chat(String userMessage) {
        OpenAiChatRequest request = OpenAiChatRequest.builder()
            .model(modelName)
            .messages(List.of(OpenAiMessage.user(userMessage)))
            .build();
        return chat(request).map(OpenAiChatResponse::getContent);
    }

    /**
     * 单轮对话（流式）
     */
    @Override
    public Flux<String> chatStream(String userMessage) {
        OpenAiChatRequest request = OpenAiChatRequest.builder()
            .model(modelName)
            .messages(List.of(OpenAiMessage.user(userMessage)))
            .stream(true)
            .build();
        return chatStream(request)
            .filter(response -> response != null && response.getContent() != null && !response.getContent().isEmpty())
            .map(OpenAiChatResponse::getContent);
    }

    /**
     * 多轮对话
     */
    @Override
    public Mono<String> chat(List<com.xhhao.aimodelhub.api.ChatMessage> messages) {
        List<OpenAiMessage> openAiMessages = messages.stream()
            .map(msg -> new OpenAiMessage(msg.getRole(), msg.getContent(), null, null, null))
            .toList();

        OpenAiChatRequest request = OpenAiChatRequest.builder()
            .model(modelName)
            .messages(openAiMessages)
            .build();

        return chat(request).map(OpenAiChatResponse::getContent);
    }

    /**
     * 多轮对话（流式）
     */
    @Override
    public Flux<String> chatStream(List<com.xhhao.aimodelhub.api.ChatMessage> messages) {
        List<OpenAiMessage> openAiMessages = messages.stream()
            .map(msg -> new OpenAiMessage(msg.getRole(), msg.getContent(), null, null, null))
            .toList();

        OpenAiChatRequest request = OpenAiChatRequest.builder()
            .model(modelName)
            .messages(openAiMessages)
            .stream(true)
            .build();

        return chatStream(request)
            .filter(response -> response != null && response.getContent() != null && !response.getContent().isEmpty())
            .map(OpenAiChatResponse::getContent);
    }

    private WebClient createWebClient() {
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
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

    private void applyDefaults(OpenAiChatRequest request) {
        if (request.getTemperature() == null && temperature != null) {
            request.setTemperature(temperature);
        }
        if (request.getTopP() == null && topP != null) {
            request.setTopP(topP);
        }
        if (request.getMaxTokens() == null && maxTokens != null) {
            request.setMaxTokens(maxTokens);
        }
        if (request.getMaxCompletionTokens() == null && maxCompletionTokens != null) {
            request.setMaxCompletionTokens(maxCompletionTokens);
        }
        if (request.getFrequencyPenalty() == null && frequencyPenalty != null) {
            request.setFrequencyPenalty(frequencyPenalty);
        }
        if (request.getPresencePenalty() == null && presencePenalty != null) {
            request.setPresencePenalty(presencePenalty);
        }
        if (request.getStop() == null && stop != null) {
            request.setStop(stop);
        }
        if (request.getSeed() == null && seed != null) {
            request.setSeed(seed);
        }
        if (request.getUser() == null && user != null) {
            request.setUser(user);
        }
        if (request.getLogitBias() == null && logitBias != null) {
            request.setLogitBias(logitBias);
        }
    }

    private OpenAiChatResponse parseResponse(String json) {
        try {
            return MAPPER.readValue(json, OpenAiChatResponse.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
