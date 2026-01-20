package com.xhhao.aimodelhub.api;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Map;

/**
 * 图像生成选项（统一配置，按需设置）
 *
 * @author Handsome
 * @since 1.0.0
 */
@Data
@Builder
public class ImageOptions {

    // ==================== 基础参数 ====================

    private String apiKey;              // API Key（使用自定义 Key 时必填）
    private String model;               // 模型名称
    private String baseUrl;             // API 地址

    // ==================== 生成参数 ====================

    private String size;                // 尺寸: 1024x1024, 1024x1792 等
    @Builder.Default
    private String quality = "standard";// 质量: standard/hd (DALL-E 3)
    @Builder.Default
    private int n = 1;                  // 生成数量
    private String style;               // 风格: vivid/natural (DALL-E 3)
    @Builder.Default
    private boolean watermark = false;  // 是否加水印

    // ==================== 网络参数 ====================

    private Duration timeout;           // 超时时间
    private Integer maxRetries;         // 最大重试次数
    private Map<String, String> customHeaders; // 自定义请求头

    // ==================== 硅基流动特有 ====================

    private Integer steps;              // 采样步数
    private Integer guidanceScale;      // 引导系数
    private Long seed;                  // 随机种子
    private String negativePrompt;      // 负面提示词
    private String imageFormat;         // 输出格式: png/jpeg

    // ==================== 工厂方法 ====================

    public static ImageOptions defaults() {
        return ImageOptions.builder().build();
    }

    public static ImageOptions of(String apiKey, String model) {
        return ImageOptions.builder().apiKey(apiKey).model(model).build();
    }

    public static ImageOptions of(String apiKey, String model, String size) {
        return ImageOptions.builder().apiKey(apiKey).model(model).size(size).build();
    }
}
