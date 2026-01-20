package com.xhhao.aimodelhub.api;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 聊天模型调用参数（统一配置，按需设置）
 *
 * @author Handsome
 * @since 1.0.0
 */
@Data
@Builder
public class ChatOptions {

    // ==================== 基础参数 ====================

    private String apiKey;              // API Key（必需）
    private String model;               // 模型名称
    private String baseUrl;             // API 地址

    // ==================== 生成参数 ====================

    private Double temperature;         // 温度 0-2，越高越随机
    private Double topP;                // 核采样 0-1
    private Integer maxTokens;          // 最大生成 Token 数
    private List<String> stop;          // 停止词
    private Double frequencyPenalty;    // 频率惩罚 -2~2
    private Double presencePenalty;     // 存在惩罚 -2~2
    private Integer seed;               // 随机种子
    private String user;                // 用户标识

    // ==================== 网络参数 ====================

    private Duration timeout;           // 统一超时
    private Duration connectTimeout;    // 连接超时
    private Duration readTimeout;       // 读取超时
    private Duration writeTimeout;      // 写入超时
    private Duration callTimeout;       // 调用超时
    private Integer maxRetries;         // 最大重试次数

    // ==================== OpenAI ====================

    private String organizationId;      // 组织 ID
    private String projectId;           // 项目 ID
    private Integer maxCompletionTokens;// o1 系列最大完成 Token
    private Map<String, Integer> logitBias; // Token 偏置
    private String responseFormat;      // 响应格式: text/json_object/json_schema
    private Boolean strictJsonSchema;   // 严格 JSON Schema
    private Boolean parallelToolCalls;  // 并行工具调用
    private Boolean strictTools;        // 严格工具模式

    // ==================== 硅基流动 ====================

    private Boolean enableThinking;     // 开启思维模式（推理模型）
    private Integer thinkingBudget;     // 思维链 Token 数 128-32768
    private Double minP;                // 动态过滤阈值 0-1（Qwen3）
    private Integer topK;               // Top-K 采样
    private Double repetitionPenalty;   // 重复惩罚
    private Integer n;                  // 生成数量

    // ==================== 智谱 ====================

    private String requestId;           // 请求追踪 ID
    private Boolean webSearch;          // 联网搜索（GLM-4）
    private String toolChoice;          // 工具选择: auto/none/required

    // ==================== 调试 ====================

    private Map<String, String> customHeaders; // 自定义请求头
    private Boolean logRequests;        // 记录请求日志
    private Boolean logResponses;       // 记录响应日志

    // ==================== 工厂方法 ====================

    public static ChatOptions of(String apiKey, String model) {
        return ChatOptions.builder().apiKey(apiKey).model(model).build();
    }

    public static ChatOptions of(String apiKey, String baseUrl, String model) {
        return ChatOptions.builder().apiKey(apiKey).baseUrl(baseUrl).model(model).build();
    }
}
