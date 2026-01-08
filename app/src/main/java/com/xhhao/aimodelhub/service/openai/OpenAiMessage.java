package com.xhhao.aimodelhub.service.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI 消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiMessage {
    private String role;
    private Object content;
    private String name;
    private List<ToolCall> toolCalls;
    private String toolCallId;

    public static OpenAiMessage system(String content) {
        return new OpenAiMessage("system", content, null, null, null);
    }

    public static OpenAiMessage user(String content) {
        return new OpenAiMessage("user", content, null, null, null);
    }

    public static OpenAiMessage assistant(String content) {
        return new OpenAiMessage("assistant", content, null, null, null);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String id;
        private String type;
        private Function function;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Function {
        private String name;
        private String arguments;
    }
}
