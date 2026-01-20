package com.xhhao.aimodelhub.endpoint;

import com.xhhao.aimodelhub.model.ModelListItem;
import com.xhhao.aimodelhub.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import java.util.Comparator;
import java.util.List;

/**
 * 模型列表 API 端点
 */
@Component
@RequiredArgsConstructor
public class ModelEndpoint implements CustomEndpoint {

    private final ModelService modelService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        String tag = groupVersion().toString();
        return SpringdocRouteBuilder.route()
            .GET("/models/{provider}", this::listModelsByProvider,
                builder -> builder.operationId("listModelsByProvider")
                    .tag(tag)
                    .description("获取指定供应商的模型列表")
                    .parameter(parameterBuilder().name("provider").in(io.swagger.v3.oas.annotations.enums.ParameterIn.PATH).required(true).description("供应商名称")))
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.aimodel-hub.xhhao.com/v1alpha1");
    }

    private Mono<ServerResponse> listModelsByProvider(ServerRequest request) {
        String provider = request.pathVariable("provider");
        String keyword = request.queryParam("keyword").orElse("");
        int page = request.queryParam("page").map(Integer::parseInt).orElse(1);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(0);

        Mono<List<ModelListItem>> modelsMono = switch (provider) {
            case "siliconflow" -> modelService.listSiliconFlowModels();
            default -> Mono.just(List.of());
        };

        return modelsMono
            .map(items -> filterAndPaginate(items, keyword, page, size))
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private ListResult<ModelListItem> filterAndPaginate(List<ModelListItem> items, String keyword, int page, int size) {
        if (items.isEmpty()) {
            return new ListResult<>(0, 0, 0L, List.of());
        }

        List<ModelListItem> filtered = items;
        if (StringUtils.isNotBlank(keyword)) {
            filtered = items.stream()
                .filter(item -> item.getValue().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
        }

        int total = filtered.size();
        int fromIndex = Math.min(Math.max(0, page - 1) * size, total);
        int toIndex = (size == 0) ? total : Math.min(fromIndex + size, total);

        List<ModelListItem> paged = filtered.subList(fromIndex, toIndex).stream()
            .sorted(Comparator.comparing(ModelListItem::getCreated).reversed())
            .toList();

        return new ListResult<>(page, size, (long) total, paged);
    }
}
