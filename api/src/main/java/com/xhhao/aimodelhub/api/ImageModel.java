package com.xhhao.aimodelhub.api;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 图像生成模型接口
 *
 * @author Handsome
 * @since 1.0.0
 */
public interface ImageModel {

    /**
     * 生成图像（使用默认选项）
     *
     * @param prompt 图像描述
     * @return 生成的图像 URL 列表
     */
    Mono<List<String>> generate(String prompt);

    /**
     * 生成图像（自定义选项）
     *
     * @param prompt  图像描述
     * @param options 生成选项
     * @return 生成的图像 URL 列表
     */
    Mono<List<String>> generate(String prompt, ImageOptions options);
}
