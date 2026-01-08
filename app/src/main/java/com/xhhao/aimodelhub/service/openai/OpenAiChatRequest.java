package com.xhhao.aimodelhub.service.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat 请求参数（完整参数）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiChatRequest {

    // ===== 必需 =====
    private List<OpenAiMessage> messages;
    private String model;

    // ===== 可选 =====
    private Boolean stream;
    
    @JsonProperty("stream_options")
    private StreamOptions streamOptions;
    
    private Double temperature;
    
    @JsonProperty("top_p")
    private Double topP;
    
    private Integer n;
    
    private List<String> stop;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;
    
    @JsonProperty("presence_penalty")
    private Double presencePenalty;
    
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;
    
    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;
    
    private Boolean logprobs;
    
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;
    
    private String user;
    
    private Integer seed;
    
    private List<Tool> tools;
    
    @JsonProperty("tool_choice")
    private Object toolChoice;
    
    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;
    
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    @JsonProperty("reasoning_effort")
    private String reasoningEffort;
    
    @JsonProperty("service_tier")
    private String serviceTier;
    
    private Boolean store;
    
    private Map<String, String> metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamOptions {
        @JsonProperty("include_usage")
        private Boolean includeUsage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tool {
        private String type;
        private FunctionDef function;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionDef {
        private String name;
        private String description;
        private Object parameters;
        private Boolean strict;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        private String type;
        @JsonProperty("json_schema")
        private JsonSchema jsonSchema;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonSchema {
        private String name;
        private String description;
        private Object schema;
        private Boolean strict;
    }
}
