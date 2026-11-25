package com.coldchain.triage;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    static final String FEED_TOPIC = "coldchain.telemetry.excursion.v1";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17.6-alpine"));
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:4.2.0"));
    }

    @Bean
    NewTopic feedTopicTheApiOwns() {
        return TopicBuilder.name(FEED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean(destroyMethod = "stop")
    FakeColdChainApi fakeColdChainApi() {
        return FakeColdChainApi.start();
    }

    @Bean
    DynamicPropertyRegistrar theApiTriageAsksForContext(FakeColdChainApi api) {
        return registry -> {
            registry.add("coldchain.triage.api.base-url", api::baseUrl);
            registry.add("coldchain.triage.api.client-secret", () -> FakeColdChainApi.SECRET);
            registry.add("coldchain.triage.api.backoff", () -> "50ms");
        };
    }
}
