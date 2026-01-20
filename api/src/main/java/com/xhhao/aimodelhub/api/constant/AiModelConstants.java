package com.xhhao.aimodelhub.api.constant;

/**
 * AI 模型相关常量
 *
 * @author Handsome
 * @since 1.0.0
 */
public final class AiModelConstants {

    private AiModelConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * 默认超时时间（秒）
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 120;

    /**
     * 默认最大重试次数
     */
    public static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 响应摘要最大长度
     */
    public static final int RESPONSE_SUMMARY_MAX_LENGTH = 200;

    /**
     * 用户消息最大长度
     */
    public static final int USER_MESSAGE_MAX_LENGTH = 500;

    /**
     * 默认最大历史消息数
     */
    public static final int DEFAULT_MAX_HISTORY = 20;

    /**
     * 默认模型名称
     */
    public static final String DEFAULT_OPENAI_MODEL = "gpt-4o";

    /**
     * 硅基流动默认模型
     */
    public static final String DEFAULT_SILICONFLOW_MODEL = "Qwen/Qwen2.5-7B-Instruct";

    /**
     * 硅基流动 API 地址
     */
    public static final String SILICONFLOW_BASE_URL = "https://api.siliconflow.cn";

    /**
     * 供应商名称
     */
    public static final class Provider {
        public static final String OPENAI = "openai";
        public static final String SILICONFLOW = "siliconflow";
        public static final String CLAUDE = "claude";
        public static final String GEMINI = "gemini";

        private Provider() {
        }
    }
}
