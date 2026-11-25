package com.coldchain.triage.delivery.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class ExcursionFeedErrorHandlingConfig {

    private static final int RETRIES = 2;

    private static final long FIRST_RETRY_AFTER_MILLIS = 500;

    private static final double BACKOFF_MULTIPLIER = 2.0;

    private static final int ANY_PARTITION = -1;

    @Bean
    NewTopic excursionDeadLetterTopic(@Value("${coldchain.triage.dead-letter-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler excursionFeedErrorHandler(KafkaTemplate<?, ?> template,
            @Value("${coldchain.triage.dead-letter-topic}") String deadLetterTopic) {
        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(template,
                (failed, cause) -> new TopicPartition(deadLetterTopic, ANY_PARTITION));
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(RETRIES);
        backOff.setInitialInterval(FIRST_RETRY_AFTER_MILLIS);
        backOff.setMultiplier(BACKOFF_MULTIPLIER);
        DefaultErrorHandler handler = new DefaultErrorHandler(deadLetter, backOff);
        handler.addNotRetryableExceptions(UnreadableFeedMessageException.class);
        return handler;
    }
}
