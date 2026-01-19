package com.xhhao.aimodelhub.api.exception;

import lombok.Getter;

/**
 * AI 模型调用异常
 *
 * @author Handsome
 * @since 1.0.0
 */
@Getter
public class AiModelException extends RuntimeException {

    /**
     * 错误码
     */
    private final ErrorCode errorCode;

    /**
     * 供应商名称
     */
    private final String provider;

    /**
     * 模型名称
     */
    private final String model;

    public AiModelException(String message, ErrorCode errorCode) {
        this(message, errorCode, null, null, null);
    }

    public AiModelException(String message, ErrorCode errorCode, String provider, String model) {
        this(message, errorCode, provider, model, null);
    }

    public AiModelException(String message, ErrorCode errorCode, String provider, String model, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.provider = provider;
        this.model = model;
    }

    /**
     * 错误码枚举
     */
    @Getter
    public enum ErrorCode {
        /**
         * 配置错误
         */
        CONFIG_ERROR("CONFIG_001", "配置错误"),
        /**
         * 认证失败
         */
        AUTH_ERROR("AUTH_001", "认证失败"),
        /**
         * API 调用失败
         */
        API_ERROR("API_001", "API 调用失败"),
        /**
         * 请求超时
         */
        TIMEOUT("TIMEOUT_001", "请求超时"),
        /**
         * 速率限制
         */
        RATE_LIMIT("RATE_001", "请求过于频繁"),
        /**
         * 模型不可用
         */
        MODEL_UNAVAILABLE("MODEL_001", "模型不可用"),
        /**
         * 参数错误
         */
        INVALID_PARAM("PARAM_001", "参数错误"),
        /**
         * 未知错误
         */
        UNKNOWN("UNKNOWN_001", "未知错误");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
    }

    /**
     * 创建配置错误异常
     */
    public static AiModelException configError(String message) {
        return new AiModelException(message, ErrorCode.CONFIG_ERROR);
    }

    /**
     * 创建 API 调用错误异常
     */
    public static AiModelException apiError(String message, String provider, String model, Throwable cause) {
        return new AiModelException(message, ErrorCode.API_ERROR, provider, model, cause);
    }

    /**
     * 创建超时异常
     */
    public static AiModelException timeout(String provider, String model) {
        return new AiModelException("请求超时", ErrorCode.TIMEOUT, provider, model, null);
    }
}
