package com.xhhao.aimodelhub.service.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Token Bucket 算法的限流服务
 * <p>
 * 支持按 IP 或用户进行限流
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
@Slf4j
@Service
public class RateLimiterService {

    /**
     * IP 限流桶
     */
    private final ConcurrentHashMap<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();

    /**
     * 用户限流桶
     */
    private final ConcurrentHashMap<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();

    /**
     * 默认配置
     */
    private volatile int maxRequestsPerMinute = 60;
    private volatile int maxRequestsPerDay = 1000;
    private volatile boolean enabled = false;

    /**
     * 检查是否允许请求（基于 IP）
     *
     * @param ip 客户端 IP
     * @return 是否允许
     */
    public boolean allowRequestByIp(String ip) {
        if (!enabled || ip == null || ip.isBlank()) {
            return true;
        }

        TokenBucket bucket = ipBuckets.computeIfAbsent(ip, 
            k -> new TokenBucket(maxRequestsPerMinute, maxRequestsPerDay));
        
        boolean allowed = bucket.tryAcquire();
        if (!allowed) {
            log.warn("IP {} 触发限流，每分钟限制: {}, 每日限制: {}", ip, maxRequestsPerMinute, maxRequestsPerDay);
        }
        return allowed;
    }

    /**
     * 检查是否允许请求（基于用户）
     *
     * @param username 用户名
     * @return 是否允许
     */
    public boolean allowRequestByUser(String username) {
        if (!enabled || username == null || username.isBlank()) {
            return true;
        }

        TokenBucket bucket = userBuckets.computeIfAbsent(username, 
            k -> new TokenBucket(maxRequestsPerMinute, maxRequestsPerDay));
        
        boolean allowed = bucket.tryAcquire();
        if (!allowed) {
            log.warn("用户 {} 触发限流，每分钟限制: {}, 每日限制: {}", username, maxRequestsPerMinute, maxRequestsPerDay);
        }
        return allowed;
    }

    /**
     * 更新限流配置
     */
    public void updateConfig(boolean enabled, int maxRequestsPerMinute, int maxRequestsPerDay) {
        this.enabled = enabled;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxRequestsPerDay = maxRequestsPerDay;
        
        // 清理旧桶，使用新配置
        ipBuckets.clear();
        userBuckets.clear();
        
        log.info("限流配置已更新: enabled={}, maxPerMinute={}, maxPerDay={}", 
            enabled, maxRequestsPerMinute, maxRequestsPerDay);
    }

    /**
     * 获取当前配置
     */
    public RateLimitConfig getConfig() {
        return new RateLimitConfig(enabled, maxRequestsPerMinute, maxRequestsPerDay);
    }

    /**
     * 清理过期的桶（可定期调用）
     */
    public void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        long expireTime = 24 * 60 * 60 * 1000L; // 24小时未使用则清理

        ipBuckets.entrySet().removeIf(entry -> 
            now - entry.getValue().getLastAccessTime() > expireTime);
        userBuckets.entrySet().removeIf(entry -> 
            now - entry.getValue().getLastAccessTime() > expireTime);
    }

    /**
     * 限流配置
     */
    public record RateLimitConfig(boolean enabled, int maxRequestsPerMinute, int maxRequestsPerDay) {}

    /**
     * Token Bucket 实现
     */
    @Getter
    private static class TokenBucket {
        private final int maxPerMinute;
        private final int maxPerDay;
        
        private final AtomicLong minuteTokens;
        private final AtomicLong dayTokens;
        
        private volatile long lastMinuteReset;
        private volatile long lastDayReset;
        private volatile long lastAccessTime;

        public TokenBucket(int maxPerMinute, int maxPerDay) {
            this.maxPerMinute = maxPerMinute;
            this.maxPerDay = maxPerDay;
            this.minuteTokens = new AtomicLong(maxPerMinute);
            this.dayTokens = new AtomicLong(maxPerDay);
            this.lastMinuteReset = System.currentTimeMillis();
            this.lastDayReset = System.currentTimeMillis();
            this.lastAccessTime = System.currentTimeMillis();
        }

        public synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            lastAccessTime = now;

            // 检查是否需要重置分钟桶
            if (now - lastMinuteReset >= 60_000) {
                minuteTokens.set(maxPerMinute);
                lastMinuteReset = now;
            }

            // 检查是否需要重置日桶
            if (now - lastDayReset >= 24 * 60 * 60 * 1000L) {
                dayTokens.set(maxPerDay);
                lastDayReset = now;
            }

            // 检查分钟限制
            if (minuteTokens.get() <= 0) {
                return false;
            }

            // 检查日限制
            if (dayTokens.get() <= 0) {
                return false;
            }

            // 消耗 token
            minuteTokens.decrementAndGet();
            dayTokens.decrementAndGet();
            return true;
        }
    }
}
