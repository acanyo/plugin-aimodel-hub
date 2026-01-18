package com.xhhao.aimodelhub.service;

import com.xhhao.aimodelhub.extension.AiChatLog;
import com.xhhao.aimodelhub.query.AiChatLogQuery;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * AI 聊天日志服务
 *
 * @author Handsome
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatLogService {

    private final ReactiveExtensionClient client;

    /**
     * 记录聊天日志
     */
    public Mono<AiChatLog> logChat(String callerPlugin, String provider, String model,
                                   String userMessage, boolean stream, long startTime,
                                   Integer promptTokens, Integer completionTokens,
                                   boolean success, String errorMessage, String response) {
        
        AiChatLog chatLog = new AiChatLog();
        
        // 设置 metadata
        Metadata metadata = new Metadata();
        metadata.setName("chatlog-" + UUID.randomUUID().toString().substring(0, 8));
        chatLog.setMetadata(metadata);
        
        // 设置 spec
        AiChatLog.AiChatLogSpec spec = new AiChatLog.AiChatLogSpec();
        spec.setCallerPlugin(callerPlugin);
        spec.setProvider(provider);
        spec.setModel(model);
        spec.setUserMessage(truncate(userMessage, 500));
        spec.setStream(stream);
        spec.setRequestTime(Instant.ofEpochMilli(startTime));
        chatLog.setSpec(spec);
        
        // 设置 status
        AiChatLog.AiChatLogStatus status = new AiChatLog.AiChatLogStatus();
        status.setPromptTokens(promptTokens);
        status.setCompletionTokens(completionTokens);
        status.setTotalTokens((promptTokens != null ? promptTokens : 0) + 
                             (completionTokens != null ? completionTokens : 0));
        status.setDurationMs(System.currentTimeMillis() - startTime);
        status.setSuccess(success);
        status.setErrorMessage(errorMessage);
        status.setResponseSummary(truncate(response, 200));
        chatLog.setStatus(status);
        
        return client.create(chatLog)
            .doOnSuccess(saved -> log.debug("Chat log saved: {}", saved.getMetadata().getName()))
            .doOnError(e -> log.error("Failed to save chat log", e));
    }

    /**
     * 查询日志列表
     */
    public Mono<ListResult<AiChatLog>> listLogs(AiChatLogQuery query) {
        return client.listAll(AiChatLog.class, query.toListOptions(), null)
            .filter(log -> {
                // 根据查询条件过滤
                if (query.getCallerPlugin() != null) {
                    if (log.getSpec() == null || 
                        !query.getCallerPlugin().equals(log.getSpec().getCallerPlugin())) {
                        return false;
                    }
                }
                if (query.getProvider() != null) {
                    if (log.getSpec() == null || 
                        !query.getProvider().equals(log.getSpec().getProvider())) {
                        return false;
                    }
                }
                if (query.getModel() != null) {
                    if (log.getSpec() == null || 
                        !query.getModel().equals(log.getSpec().getModel())) {
                        return false;
                    }
                }
                if (query.getSuccess() != null) {
                    if (log.getStatus() == null || 
                        !query.getSuccess().equals(log.getStatus().getSuccess())) {
                        return false;
                    }
                }
                return true;
            })
            .collectList()
            .map(logs -> {
                // 手动分页
                int page = query.getPage();
                int size = query.getSize();
                int total = logs.size();
                int start = (page - 1) * size;
                int end = Math.min(start + size, total);
                
                var items = (start < total) ? logs.subList(start, end) : java.util.List.<AiChatLog>of();
                
                return new ListResult<>(page, size, total, items);
            });
    }

    /**
     * 获取统计信息
     */
    public Mono<AiChatLogStats> getStats() {
        return client.listAll(AiChatLog.class, new ListOptions(), null)
            .collectList()
            .map(logs -> {
                var stats = new AiChatLogStats();
                stats.setTotalCalls(logs.size());
                
                int successCount = 0;
                long totalPromptTokens = 0;
                long totalCompletionTokens = 0;
                long totalTokens = 0;
                long todayTokens = 0;
                int todayCalls = 0;
                
                LocalDate today = LocalDate.now();
                
                for (var log : logs) {
                    var status = log.getStatus();
                    if (status != null) {
                        if (Boolean.TRUE.equals(status.getSuccess())) {
                            successCount++;
                        }
                        if (status.getPromptTokens() != null) {
                            totalPromptTokens += status.getPromptTokens();
                        }
                        if (status.getCompletionTokens() != null) {
                            totalCompletionTokens += status.getCompletionTokens();
                        }
                        if (status.getTotalTokens() != null) {
                            totalTokens += status.getTotalTokens();
                        }
                    }
                    
                    // 统计今日
                    var spec = log.getSpec();
                    if (spec != null && spec.getRequestTime() != null) {
                        LocalDate requestDate = spec.getRequestTime()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                        if (requestDate.equals(today)) {
                            todayCalls++;
                            if (status != null && status.getTotalTokens() != null) {
                                todayTokens += status.getTotalTokens();
                            }
                        }
                    }
                }
                
                stats.setSuccessCount(successCount);
                stats.setFailCount(logs.size() - successCount);
                stats.setTotalPromptTokens(totalPromptTokens);
                stats.setTotalCompletionTokens(totalCompletionTokens);
                stats.setTotalTokens(totalTokens);
                stats.setTodayCalls(todayCalls);
                stats.setTodayTokens(todayTokens);
                
                return stats;
            });
    }

    /**
     * 删除日志
     */
    public Mono<Void> deleteLog(String name) {
        return client.fetch(AiChatLog.class, name)
            .flatMap(client::delete)
            .then();
    }

    /**
     * 截取字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 统计信息
     */
    @Data
    public static class AiChatLogStats {
        private int totalCalls;
        private int successCount;
        private int failCount;
        private long totalPromptTokens;
        private long totalCompletionTokens;
        private long totalTokens;
        private int todayCalls;
        private long todayTokens;
    }
}
