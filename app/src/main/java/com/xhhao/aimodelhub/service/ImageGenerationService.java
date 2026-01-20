package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.api.ImageModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 图像生成服务
 * <p>
 * 支持多种图像生成供应商
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private final ImageModelFactory imageModelFactory;

    /**
     * 生成图像（使用指定供应商）
     *
     * @param provider 供应商 (openai/zhipu/siliconflow)
     * @param prompt   图像描述
     * @return 生成的图像 URL 列表
     */
    public Mono<List<String>> generateImage(String provider, String prompt) {
        String actualProvider = (provider == null || provider.isBlank()) ? "siliconflow" : provider.toLowerCase();
        
        return switch (actualProvider) {
            case "openai" -> imageModelFactory.openai().flatMap(model -> model.generate(prompt));
            case "zhipu" -> imageModelFactory.zhipu().flatMap(model -> model.generate(prompt));
            case "siliconflow" -> imageModelFactory.siliconflow().flatMap(model -> model.generate(prompt));
            default -> Mono.error(new IllegalArgumentException("不支持的图像供应商: " + actualProvider));
        };
    }
}
