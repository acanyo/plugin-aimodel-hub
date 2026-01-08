package com.xhhao.aimodelhub.service.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI Chat 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChatResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;
    
    @JsonProperty("service_tier")
    private String serviceTier;

    /** 获取回复内容 */
    public String getContent() {
        if (choices == null || choices.isEmpty()) return null;
        Choice choice = choices.get(0);
        if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
            return choice.getMessage().getContent().toString();
        }
        if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
            return choice.getDelta().getContent().toString();
        }
        return null;
    }

    /** 获取完成原因 */
    public String getFinishReason() {
        if (choices == null || choices.isEmpty()) return null;
        return choices.get(0).getFinishReason();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Integer index;
        private OpenAiMessage message;
        private OpenAiMessage delta;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
