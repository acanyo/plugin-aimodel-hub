package com.xhhao.aimodelhub.service.common;

import com.xhhao.aimodelhub.api.ImageModel;
import com.xhhao.aimodelhub.api.ImageOptions;
import com.xhhao.aimodelhub.extension.AiChatLog;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 带日志记录的 ImageModel 包装器
 * <p>
 * 使用装饰器模式，透明地为图像生成调用添加日志记录功能。
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
@Slf4j
public class LoggingImageModel implements ImageModel {

    private final ImageModel delegate;
    private final AiChatLogService logService;
    private final String provider;
    private final String model;

    public LoggingImageModel(ImageModel delegate, AiChatLogService logService, 
                             String provider, String model) {
        this.delegate = delegate;
        this.logService = logService;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public Mono<List<String>> generate(String prompt) {
        return generate(prompt, ImageOptions.defaults());
    }

    @Override
    public Mono<List<String>> generate(String prompt, ImageOptions options) {
        long startTime = System.currentTimeMillis();

        return delegate.generate(prompt, options)
            .doOnSuccess(urls -> asyncLogSuccess(prompt, startTime, urls))
            .doOnError(e -> asyncLogError(prompt, startTime, e));
    }

    /**
     * 异步记录成功日志
     */
    private void asyncLogSuccess(String prompt, long startTime, List<String> imageUrls) {
        executeAsync(() -> {
            String response = imageUrls != null && !imageUrls.isEmpty() 
                ? String.format("生成 %d 张图像: %s", imageUrls.size(), String.join(", ", imageUrls))
                : "无图像生成";

            logService.logChat(null, provider, model,
                    prompt, AiChatLog.CallType.IMAGE, startTime, null, null,
                    true, null, response)
                .subscribe(
                    saved -> log.debug("图像日志已保存: {}", saved.getMetadata().getName()),
                    e -> log.warn("保存图像日志失败", e)
                );
        });
    }

    /**
     * 异步记录错误日志
     */
    private void asyncLogError(String prompt, long startTime, Throwable error) {
        executeAsync(() -> logService.logChat(null, provider, model,
                prompt, AiChatLog.CallType.IMAGE, startTime, null, null,
                false, error.getMessage(), null)
            .subscribe(
                saved -> log.debug("图像错误日志已保存: {}", saved.getMetadata().getName()),
                e -> log.warn("保存图像错误日志失败", e)
            ));
    }

    /**
     * 在弹性线程池中异步执行任务
     */
    private void executeAsync(Runnable task) {
        Mono.fromRunnable(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("异步任务执行失败", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }
}
