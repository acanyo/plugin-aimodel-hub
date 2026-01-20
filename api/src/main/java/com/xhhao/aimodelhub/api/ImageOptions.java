package com.xhhao.aimodelhub.api;

import lombok.Builder;
import lombok.Data;

/**
 * 图像生成选项
 *
 * @author Handsome
 * @since 1.0.0
 */
@Data
@Builder
public class ImageOptions {

    /**
     * 图像尺寸，如 1024x1024, 1024x1792, 1792x1024
     */
    private String size;

    /**
     * 图像质量：standard 或 hd (仅 DALL-E 3)
     */
    @Builder.Default
    private String quality = "standard";

    /**
     * 生成数量 (DALL-E 3 只支持 1)
     */
    @Builder.Default
    private int n = 1;

    /**
     * 图像风格：vivid 或 natural (仅 DALL-E 3)
     */
    private String style;

    /**
     * 是否添加水印（部分供应商支持）
     */
    @Builder.Default
    private boolean watermark = false;

    /**
     * 默认选项
     */
    public static ImageOptions defaults() {
        return ImageOptions.builder().build();
    }
}
