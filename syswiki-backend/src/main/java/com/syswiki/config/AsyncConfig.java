package com.syswiki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 *
 * 为 @Async 注解提供专用的线程池，替代默认的 SimpleAsyncTaskExecutor。
 * 默认 SimpleAsyncTaskExecutor 每次调用都创建新线程，高并发下会耗尽系统资源。
 *
 * 线程池参数可通过环境变量配置：
 *   ASYNC_CORE_POOL_SIZE  - 核心线程数（默认 2）
 *   ASYNC_MAX_POOL_SIZE   - 最大线程数（默认 4）
 *   ASYNC_QUEUE_CAPACITY  - 队列容量（默认 100）
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${syswiki.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${syswiki.async.max-pool-size:4}")
    private int maxPoolSize;

    @Value("${syswiki.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean("asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("vector-sync-");
        // 队列满时由调用线程执行，不丢弃任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
