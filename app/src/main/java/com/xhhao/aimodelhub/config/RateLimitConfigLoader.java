package com.xhhao.aimodelhub.config;

import com.xhhao.aimodelhub.service.common.RateLimiterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 限流配置加载器
 * <p>
 * 启动时加载配置，定期刷新配置
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitConfigLoader {

    private final SettingConfigGetter configGetter;
    private final RateLimiterService rateLimiterService;

    /**
     * 插件启动时加载配置
     */
    @PostConstruct
    public void init() {
        loadConfig();
    }

    /**
     * 定期刷新配置（每分钟）
     */
    @Scheduled(fixedRate = 60000)
    public void refreshConfig() {
        loadConfig();
    }

    /**
     * 加载配置并更新限流服务
     */
    private void loadConfig() {
        configGetter.getSecurityConfig()
            .subscribe(
                config -> {
                    var rateLimit = config.getRateLimit();
                    if (rateLimit == null) {
                        log.debug("限流配置为空，使用默认配置");
                        return;
                    }
                    
                    boolean enabled = Boolean.TRUE.equals(rateLimit.getEnabled());
                    int maxPerMinute = rateLimit.getMaxRequestsPerMinute() != null 
                        ? rateLimit.getMaxRequestsPerMinute() : 60;
                    int maxPerDay = rateLimit.getMaxRequestsPerDay() != null 
                        ? rateLimit.getMaxRequestsPerDay() : 1000;
                    
                    rateLimiterService.updateConfig(enabled, maxPerMinute, maxPerDay);
                },
                error -> log.warn("加载限流配置失败，使用默认配置", error)
            );
    }
}
