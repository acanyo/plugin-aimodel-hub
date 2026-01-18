package com.xhhao.aimodelhub.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.time.Instant;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * AI 聊天日志自定义模型
 * <p>
 * 记录每次 AI 调用的详细信息，包括 token 使用量
 * </p>
 *
 * @author Handsome
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "aimodel-hub.xhhao.com",
    version = "v1alpha1",
    kind = "AiChatLog",
    plural = "aichatlogs",
    singular = "aichatlog")
public class AiChatLog extends AbstractExtension {

    @Schema(requiredMode = REQUIRED)
    private AiChatLogSpec spec;

    private AiChatLogStatus status;

    @Data
    public static class AiChatLogSpec {

        /**
         * 调用者插件名称
         */
        @Schema(description = "调用者插件名称")
        private String callerPlugin;

        /**
         * 模型供应商（openai/claude/gemini）
         */
        @Schema(description = "模型供应商", requiredMode = REQUIRED)
        private String provider;

        /**
         * 使用的模型名称
         */
        @Schema(description = "使用的模型名称", requiredMode = REQUIRED)
        private String model;

        /**
         * 用户消息（简要记录，避免存储过大）
         */
        @Schema(description = "用户消息摘要")
        private String userMessage;

        /**
         * 是否流式请求
         */
        @Schema(description = "是否流式请求")
        private Boolean stream;

        /**
         * 请求时间
         */
        @Schema(description = "请求时间", requiredMode = REQUIRED)
        private Instant requestTime;
    }

    @Data
    public static class AiChatLogStatus {

        /**
         * 提示词 token 数
         */
        @Schema(description = "提示词 token 数")
        private Integer promptTokens;

        /**
         * 完成 token 数
         */
        @Schema(description = "完成 token 数")
        private Integer completionTokens;

        /**
         * 总 token 数
         */
        @Schema(description = "总 token 数")
        private Integer totalTokens;

        /**
         * 请求耗时（毫秒）
         */
        @Schema(description = "请求耗时（毫秒）")
        private Long durationMs;

        /**
         * 是否成功
         */
        @Schema(description = "是否成功")
        private Boolean success;

        /**
         * 错误信息（如果失败）
         */
        @Schema(description = "错误信息")
        private String errorMessage;

        /**
         * 响应内容摘要（截取前 200 字符）
         */
        @Schema(description = "响应内容摘要")
        private String responseSummary;
    }
}
