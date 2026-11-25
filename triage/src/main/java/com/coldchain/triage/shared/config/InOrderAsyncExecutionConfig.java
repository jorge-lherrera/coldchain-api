package com.coldchain.triage.shared.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Spring Modulith hands each publication to an asynchronous task. On a pool, two triages of one
 * shipment committed a moment apart could reach Kafka in the opposite order; one thread keeps the
 * order in which they were committed, as it does for the API's feed (ADR-009).
 */
@Configuration
public class InOrderAsyncExecutionConfig implements AsyncConfigurer {

    private static final int SHUTDOWN_GRACE_SECONDS = 30;

    @Bean
    ThreadPoolTaskExecutor inOrderTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("in-order-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SHUTDOWN_GRACE_SECONDS);
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return inOrderTaskExecutor();
    }
}
