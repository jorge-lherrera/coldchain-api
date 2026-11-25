package com.coldchain.triage.internal.infrastructure.messaging;

import com.coldchain.triage.api.event.AlertTriaged;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * Every triage leaves through the outbox: the publication is written in the transaction that saves the
 * triage, and Spring Modulith sends it to Kafka once that transaction commits. Keyed by shipment, so the
 * alerts of one shipment keep their order on one partition.
 */
@Configuration
public class AlertFeedConfig {

    @Bean
    EventExternalizationConfiguration alertFeed(@Value("${coldchain.triage.alert-topic}") String topic) {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(AlertTriaged.class)
                .route(AlertTriaged.class,
                        alert -> RoutingTarget.forTarget(topic).andKey(alert.shipmentId().toString()))
                .mapping(AlertTriaged.class, AlertFeedMessage::of)
                .build();
    }

    @Bean
    NewTopic alertTopic(@Value("${coldchain.triage.alert-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
