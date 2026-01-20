package com.xhhao.aimodelhub.endpoint;

import com.xhhao.aimodelhub.api.ChatModel;
import com.xhhao.aimodelhub.api.ChatModelFactory;
import com.xhhao.aimodelhub.service.common.RateLimiterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

import java.util.Map;

/**
 * 测试聊天端点
 * <p>
 * 用于测试 AI 调用和日志记录功能
 * </p>
 *
 * @author Handsome
 */
@Component
@RequiredArgsConstructor
public class TestChatEndpoint implements CustomEndpoint {

    private final ChatModelFactory chatModelFactory;
    private final RateLimiterService rateLimiterService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = "TestChatV1alpha1Console";
        return SpringdocRouteBuilder.route()
            .POST("/testchat/simple", this::testSimpleChat,
                builder -> builder.operationId("TestSimpleChat")
                    .description("测试简单对话（会记录日志）")
                    .tag(tag))
            .POST("/testchat/stream", this::testStreamChat,
                builder -> builder.operationId("TestStreamChat")
                    .description("测试流式对话（会记录日志）")
                    .tag(tag))
            .build();
    }

    /**
     * 测试简单对话
     */
    private Mono<ServerResponse> testSimpleChat(ServerRequest request) {
        // 限流检查
        String clientIp = getClientIp(request);
        if (!rateLimiterService.allowRequestByIp(clientIp)) {
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .bodyValue(Map.of("success", false, "message", "请求过于频繁，请稍后再试"));
        }

        return request.bodyToMono(TestChatRequest.class)
            .flatMap(req -> {
                // 根据供应商获取模型（响应式）
                return getChatModelReactive(req.getProvider(), req.getModel())
                    .flatMap(model -> 
                        // 调用 AI
                        model.chat(req.getMessage())
                            .map(response -> {
                                TestChatResponse result = new TestChatResponse();
                                result.setSuccess(true);
                                result.setMessage("调用成功，日志已记录");
                                result.setResponse(response);
                                return result;
                            })
                            .onErrorResume(e -> {
                                TestChatResponse result = new TestChatResponse();
                                result.setSuccess(false);
                                result.setMessage("调用失败: " + e.getMessage());
                                return Mono.just(result);
                            })
                    )
                    .onErrorResume(e -> {
                        TestChatResponse result = new TestChatResponse();
                        result.setSuccess(false);
                        result.setMessage("获取模型失败: " + e.getMessage());
                        return Mono.just(result);
                    });
            })
            .flatMap(result -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(result)
            );
    }

    /**
     * 测试流式对话
     */
    private Mono<ServerResponse> testStreamChat(ServerRequest request) {
        // 限流检查
        String clientIp = getClientIp(request);
        if (!rateLimiterService.allowRequestByIp(clientIp)) {
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .bodyValue(Map.of("success", false, "message", "请求过于频繁，请稍后再试"));
        }

        return request.bodyToMono(TestChatRequest.class)
            .flatMap(req -> 
                // 根据供应商获取模型（响应式）
                getChatModelReactive(req.getProvider(), req.getModel())
                    .flatMap(model -> {
                        // 流式调用
                        var flux = model.chatStream(req.getMessage());
                        
                        return ServerResponse.ok()
                            .contentType(MediaType.TEXT_EVENT_STREAM)
                            .body(flux, String.class);
                    })
                    .onErrorResume(e -> 
                        ServerResponse.badRequest()
                            .bodyValue("获取模型失败: " + e.getMessage())
                    )
            );
    }
    
    /**
     * 根据供应商获取模型
     */
    private Mono<ChatModel> getChatModelReactive(String provider, String model) {
        String actualProvider = (provider == null || provider.isBlank()) ? "siliconflow" : provider.toLowerCase();
        
        return switch (actualProvider) {
            case "openai" -> chatModelFactory.openai();
            case "siliconflow" -> chatModelFactory.siliconflow();
            case "zhipu" -> chatModelFactory.zhipu();
            default -> Mono.error(new IllegalArgumentException("不支持的供应商: " + actualProvider));
        };
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(ServerRequest request) {
        String xff = request.headers().firstHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.headers().firstHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.remoteAddress()
            .map(addr -> addr.getAddress().getHostAddress())
            .orElse("unknown");
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.aimodel-hub.xhhao.com/v1alpha1");
    }

    /**
     * 测试请求
     */
    @Data
    public static class TestChatRequest {
        /**
         * 消息内容
         */
        private String message;
        
        /**
         * 供应商（openai, claude, gemini 等）
         */
        private String provider = "openai";
        
        /**
         * 模型名称（可选，不填使用默认模型）
         */
        private String model;
    }

    /**
     * 测试响应
     */
    @Data
    public static class TestChatResponse {
        /**
         * 是否成功
         */
        private boolean success;
        
        /**
         * 提示信息
         */
        private String message;
        
        /**
         * AI 响应内容
         */
        private String response;
    }
}
