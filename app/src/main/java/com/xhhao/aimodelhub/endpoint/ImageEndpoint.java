package com.xhhao.aimodelhub.endpoint;

import com.xhhao.aimodelhub.service.ImageGenerationService;
import com.xhhao.aimodelhub.service.common.RateLimiterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

import java.util.Map;

/**
 * 图像生成 API 端点
 */
@Component
@RequiredArgsConstructor
public class ImageEndpoint implements CustomEndpoint {

    private final ImageGenerationService imageService;
    private final RateLimiterService rateLimiterService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        String tag = groupVersion().toString();
        return SpringdocRouteBuilder.route()
            .POST("/images/generate", this::generateImage,
                builder -> builder.operationId("generateImage")
                    .tag(tag)
                    .description("生成图像"))
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.aimodel-hub.xhhao.com/v1alpha1");
    }

    private Mono<ServerResponse> generateImage(ServerRequest request) {
        // 获取客户端 IP 进行限流检查
        String clientIp = getClientIp(request);
        if (!rateLimiterService.allowRequestByIp(clientIp)) {
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .bodyValue(Map.of("success", false, "message", "请求过于频繁，请稍后再试"));
        }

        return request.bodyToMono(ImageGenerateRequest.class)
            .flatMap(req -> {
                if (req.getPrompt() == null || req.getPrompt().isBlank()) {
                    return ServerResponse.badRequest()
                        .bodyValue(Map.of("success", false, "message", "请输入图像描述"));
                }

                return imageService.generateImage(req.getProvider(), req.getPrompt())
                    .flatMap(urls -> ServerResponse.ok()
                        .bodyValue(Map.of(
                            "success", true,
                            "images", urls
                        )))
                    .onErrorResume(e -> ServerResponse.badRequest()
                        .bodyValue(Map.of(
                            "success", false,
                            "message", e.getMessage()
                        )));
            });
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(ServerRequest request) {
        // 优先从 X-Forwarded-For 获取
        String xff = request.headers().firstHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        // 其次从 X-Real-IP 获取
        String realIp = request.headers().firstHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        // 最后从远程地址获取
        return request.remoteAddress()
            .map(addr -> addr.getAddress().getHostAddress())
            .orElse("unknown");
    }

    @Data
    public static class ImageGenerateRequest {
        private String prompt;
        private String provider;
    }
}
