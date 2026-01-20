package com.xhhao.aimodelhub.service.common;

import com.xhhao.aimodelhub.api.constant.AiModelConstants;
import com.xhhao.aimodelhub.extension.AiChatLog;
import com.xhhao.aimodelhub.query.AiChatLogQuery;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 聊天日志服务
 * <p>
 * 负责记录和查询 AI 调用日志
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatLogService {

    private static final String LOG_NAME_PREFIX = "chatlog-";
    private static final int LOG_NAME_SUFFIX_LENGTH = 8;

    private final ReactiveExtensionClient client;

    /**
     * 记录聊天日志
     */
    public Mono<AiChatLog> logChat(String callerPlugin, String provider, String model,
                                   String userMessage, AiChatLog.CallType callType, long startTime,
                                   Integer promptTokens, Integer completionTokens,
                                   boolean success, String errorMessage, String response) {
        
        AiChatLog chatLog = new AiChatLog();
        
        // 设置 metadata
        Metadata metadata = new Metadata();
        metadata.setName(LOG_NAME_PREFIX + UUID.randomUUID().toString().substring(0, LOG_NAME_SUFFIX_LENGTH));
        chatLog.setMetadata(metadata);
        
        // 设置 spec
        AiChatLog.AiChatLogSpec spec = new AiChatLog.AiChatLogSpec();
        spec.setCallerPlugin(callerPlugin);
        spec.setProvider(provider);
        spec.setModel(model);
        spec.setUserMessage(truncate(userMessage, AiModelConstants.USER_MESSAGE_MAX_LENGTH));
        spec.setCallType(callType);
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
        status.setResponseSummary(truncate(response, AiModelConstants.RESPONSE_SUMMARY_MAX_LENGTH));
        chatLog.setStatus(status);
        
        return client.create(chatLog)
            .doOnSuccess(saved -> log.debug("Chat log saved: {}", saved.getMetadata().getName()))
            .doOnError(e -> log.error("Failed to save chat log", e));
    }

    /**
     * 查询日志列表
     */
    public Mono<ListResult<AiChatLog>> listLogs(AiChatLogQuery query) {
        var sort = Sort.by(Sort.Order.desc("metadata.creationTimestamp"));
        var pageRequest = PageRequestImpl.of(query.getPage(), query.getSize(), sort);
        
        return client.listBy(AiChatLog.class, query.toListOptions(), pageRequest);
    }

    /**
     * 获取统计信息
     */
    public Mono<AiChatLogStats> getStats() {
        LocalDate today = LocalDate.now();
        
        return client.listAll(AiChatLog.class, new ListOptions(), null)
            .reduce(new AiChatLogStats(), (stats, log) -> {
                stats.setTotalCalls(stats.getTotalCalls() + 1);
                
                Optional.ofNullable(log.getStatus()).ifPresent(status -> {
                    if (Boolean.TRUE.equals(status.getSuccess())) {
                        stats.setSuccessCount(stats.getSuccessCount() + 1);
                    } else {
                        stats.setFailCount(stats.getFailCount() + 1);
                    }
                    stats.setTotalPromptTokens(stats.getTotalPromptTokens() + 
                        Optional.ofNullable(status.getPromptTokens()).orElse(0));
                    stats.setTotalCompletionTokens(stats.getTotalCompletionTokens() + 
                        Optional.ofNullable(status.getCompletionTokens()).orElse(0));
                    stats.setTotalTokens(stats.getTotalTokens() + 
                        Optional.ofNullable(status.getTotalTokens()).orElse(0));
                });
                
                // 统计今日
                Optional.ofNullable(log.getSpec())
                    .map(AiChatLog.AiChatLogSpec::getRequestTime)
                    .map(time -> time.atZone(ZoneId.systemDefault()).toLocalDate())
                    .filter(today::equals)
                    .ifPresent(date -> {
                        stats.setTodayCalls(stats.getTodayCalls() + 1);
                        stats.setTodayTokens(stats.getTodayTokens() + 
                            Optional.ofNullable(log.getStatus())
                                .map(AiChatLog.AiChatLogStatus::getTotalTokens)
                                .orElse(0));
                    });
                
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
