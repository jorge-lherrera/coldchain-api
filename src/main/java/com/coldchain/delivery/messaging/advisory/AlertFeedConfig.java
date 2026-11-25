package com.coldchain.delivery.messaging.advisory;

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

/**
 * The alert topic is declared here too, with the shape triage gives it, so the order in which the two
 * services start decides nothing: whichever comes first creates it. Three blocking attempts with an
 * exponential pause, then the dead letter topic; an unreadable message skips the attempts, because
 * reading the same bytes again cannot succeed. The dead letter record keeps the original bytes.
 */
@Configuration
public class AlertFeedConfig {

    private static final int ALERT_PARTITIONS = 3;

    private static final int RETRIES = 2;

    private static final long FIRST_RETRY_AFTER_MILLIS = 500;

    private static final double BACKOFF_MULTIPLIER = 2.0;

    private static final int ANY_PARTITION = -1;

    @Bean
    NewTopic alertTopic(@Value("${coldchain.advisory.alert-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(ALERT_PARTITIONS).replicas(1).build();
    }

    @Bean
    NewTopic alertDeadLetterTopic(@Value("${coldchain.advisory.dead-letter-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler alertFeedErrorHandler(KafkaTemplate<?, ?> template,
            @Value("${coldchain.advisory.dead-letter-topic}") String deadLetterTopic) {
        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(template,
                (failed, cause) -> new TopicPartition(deadLetterTopic, ANY_PARTITION));
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(RETRIES);
        backOff.setInitialInterval(FIRST_RETRY_AFTER_MILLIS);
        backOff.setMultiplier(BACKOFF_MULTIPLIER);
        DefaultErrorHandler handler = new DefaultErrorHandler(deadLetter, backOff);
        handler.addNotRetryableExceptions(UnreadableAlertMessageException.class);
        return handler;
    }
}
