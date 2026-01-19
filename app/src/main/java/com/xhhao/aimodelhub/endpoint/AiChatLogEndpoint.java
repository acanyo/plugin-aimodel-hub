package com.xhhao.aimodelhub.endpoint;

import com.xhhao.aimodelhub.query.AiChatLogQuery;
import com.xhhao.aimodelhub.service.AiChatLogService;
import lombok.RequiredArgsConstructor;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

/**
 * AI 聊天日志 API 端点
 *
 * @author Handsome
 */
@Component
@RequiredArgsConstructor
public class AiChatLogEndpoint implements CustomEndpoint {

    private final AiChatLogService chatLogService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = "AiChatLogV1alpha1Console";
        return SpringdocRouteBuilder.route()
            .GET("/aichatlogs", this::listLogs,
                builder -> builder.operationId("ListAiChatLogs")
                    .tag(tag)
                    .description("获取 AI 聊天日志列表")
                    .parameter(parameterBuilder().name("page").description("页码").required(false))
                    .parameter(parameterBuilder().name("size").description("每页数量").required(false))
                    .parameter(parameterBuilder().name("callerPlugin").description("调用者插件").required(false))
                    .parameter(parameterBuilder().name("provider").description("模型供应商").required(false))
                    .parameter(parameterBuilder().name("model").description("模型名称").required(false))
                    .parameter(parameterBuilder().name("success").description("是否成功").required(false)))
            .GET("/aichatlogs/stats", this::getStats,
                builder -> builder.operationId("GetAiChatLogStats")
                    .tag(tag)
                    .description("获取 AI 调用统计"))
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.aimodel-hub.xhhao.com/v1alpha1");
    }

    /**
     * 获取日志列表
     */
    private Mono<ServerResponse> listLogs(ServerRequest request) {
        AiChatLogQuery query = AiChatLogQuery.from(request);
        return chatLogService.listLogs(query)
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    /**
     * 获取统计信息
     */
    private Mono<ServerResponse> getStats(ServerRequest request) {
        return chatLogService.getStats()
            .flatMap(stats -> ServerResponse.ok().bodyValue(stats));
    }

}
