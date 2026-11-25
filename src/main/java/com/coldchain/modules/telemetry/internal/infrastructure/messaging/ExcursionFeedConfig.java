package com.coldchain.modules.telemetry.internal.infrastructure.messaging;

import com.coldchain.modules.telemetry.api.event.TelemetryEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

@Configuration
public class ExcursionFeedConfig {

    static final String TOPIC = "coldchain.telemetry.excursion.v1";

    @Bean
    EventExternalizationConfiguration excursionFeed() {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(TelemetryEvent.class)
                .route(TelemetryEvent.class,
                        event -> RoutingTarget.forTarget(TOPIC).andKey(event.shipmentId().toString()))
                .mapping(TelemetryEvent.class, ExcursionFeedMessage::of)
                .build();
    }

    @Bean
    NewTopic excursionTopic() {
        return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
    }
}
