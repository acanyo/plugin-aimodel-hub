package com.xhhao.aimodelhub.query;

import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;

/**
 * AI 聊天日志查询参数
 *
 * @author Handsome
 */
@Data
public class AiChatLogQuery {

    /**
     * 页码（从 1 开始）
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer size = 20;

    /**
     * 调用者插件名称
     */
    private String callerPlugin;

    /**
     * 模型供应商
     */
    private String provider;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 从 ServerRequest 构建查询参数
     */
    public static AiChatLogQuery from(ServerRequest request) {
        AiChatLogQuery query = new AiChatLogQuery();
        query.setPage(request.queryParam("page").map(Integer::parseInt).orElse(1));
        query.setSize(request.queryParam("size").map(Integer::parseInt).orElse(20));
        query.setCallerPlugin(request.queryParam("callerPlugin").orElse(null));
        query.setProvider(request.queryParam("provider").orElse(null));
        query.setModel(request.queryParam("model").orElse(null));
        query.setSuccess(request.queryParam("success").map(Boolean::parseBoolean).orElse(null));
        return query;
    }

    /**
     * 转换为 ListOptions
     */
    public ListOptions toListOptions() {
        // 暂时返回空的 ListOptions，在 Service 层进行过滤
        return new ListOptions();
    }

    /**
     * 转换为 PageRequest
     */
    public PageRequest toPageRequest() {
        return PageRequestImpl.of(page, size);
    }
}
