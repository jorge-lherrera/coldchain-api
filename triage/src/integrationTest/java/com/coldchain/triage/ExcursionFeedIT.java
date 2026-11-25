package com.coldchain.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ExcursionFeedIT {

    private static final String DEAD_LETTER_TOPIC = "coldchain.triage.excursion-dlt.v1";

    private static final Duration PATIENCE = Duration.ofSeconds(30);

    private static final Instant OPENED = Instant.parse("2026-05-01T10:05:00Z");

    private static final Instant CLOSED = Instant.parse("2026-05-01T10:50:00Z");

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private KafkaContainer broker;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anOpenedExcursionBecomesAnOpenAlertTriagedByTheRules() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID event = UUID.randomUUID();

        publish(shipment, opened(event, shipment, excursion, "9.50"));

        Map<String, Object> alert = awaitAlert(excursion);
        assertThat(alert.get("status")).isEqualTo("OPEN");
        assertThat(alert.get("source")).isEqualTo("RULES");
        assertThat(alert.get("probable_cause")).isEqualTo("UNKNOWN");
        assertThat(alert.get("severity")).isEqualTo("MEDIUM");
        assertThat(alert.get("summary_es")).asString().contains("9,5 °C");
        assertThat(inboxStatusOf(event)).isEqualTo("PROCESSED");
    }

    @Test
    void theSameEventTwiceIsOneAlertAndOneInboxRow() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        UUID marker = UUID.randomUUID();
        String once = opened(event, shipment, excursion, "9.50");

        publish(shipment, once);
        publish(shipment, once);
        publish(shipment, opened(UUID.randomUUID(), shipment, marker, "8.20"));

        awaitAlert(marker);
        assertThat(count("SELECT count(*) FROM excursion_alert WHERE excursion_id = ?", excursion))
                .describedAs("Kafka delivers at least once, and the inbox is what makes it exactly one alert")
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM inbox_event WHERE event_id = ?", event)).isEqualTo(1);
    }

    @Test
    void aCloseBeforeItsOpenIsAClosedAlertAndTheLateOpenOnlyFillsIn() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();

        publish(shipment, closed(UUID.randomUUID(), shipment, excursion, 45));
        Map<String, Object> closedFirst = awaitAlert(excursion);
        assertThat(closedFirst.get("status")).isEqualTo("CLOSED");
        assertThat(closedFirst.get("peak_celsius")).isNull();

        publish(shipment, opened(UUID.randomUUID(), shipment, excursion, "11.50"));
        await().atMost(PATIENCE).until(() -> alertOf(excursion).get("peak_celsius") != null);

        Map<String, Object> filledIn = alertOf(excursion);
        assertThat(filledIn.get("status"))
                .describedAs("an open that happened before the close cannot reopen the excursion")
                .isEqualTo("CLOSED");
        assertThat(filledIn.get("duration_minutes")).isEqualTo(45);
        assertThat(filledIn.get("severity")).isEqualTo("HIGH");
        assertThat(filledIn.get("summary_en")).asString().contains("Highest reading: 11.5 °C.");
    }

    @Test
    void aMalformedMessageIsDeadLetteredAndTheNextOneStillArrives() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        String poison = "{not json " + UUID.randomUUID();

        publish(shipment, poison);
        publish(shipment, opened(UUID.randomUUID(), shipment, excursion, "9.50"));

        awaitAlert(excursion);
        List<ConsumerRecord<String, String>> deadLettered = deadLetterUntil(poison);
        assertThat(deadLettered)
                .describedAs("a message that can never be read leaves the partition instead of blocking it")
                .anySatisfy(record -> {
                    assertThat(record.value()).isEqualTo(poison);
                    assertThat(record.key()).isEqualTo(shipment.toString());
                });
    }

    @Test
    void anEventReceivedButNeverProcessedIsTriagedWhenKafkaRedeliversIt() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inbox_event (event_id, type, excursion_id, occurred_at, received_at, status)
                VALUES (?, 'ExcursionOpened', ?, ?, now(), 'RECEIVED')""",
                event, excursion, Timestamp.from(OPENED));

        publish(shipment, opened(event, shipment, excursion, "9.50"));

        awaitAlert(excursion);
        assertThat(inboxStatusOf(event))
                .describedAs("a process that died halfway left the event RECEIVED, and the redelivery finishes it")
                .isEqualTo("PROCESSED");
    }

    private void publish(UUID shipment, String payload) {
        try {
            kafka.send(TestcontainersConfiguration.FEED_TOPIC, shipment.toString(), payload).get(10, TimeUnit.SECONDS);
        } catch (Exception cause) {
            throw new IllegalStateException("Could not publish to the feed", cause);
        }
    }

    private Map<String, Object> awaitAlert(UUID excursion) {
        await().atMost(PATIENCE).until(() -> count(
                "SELECT count(*) FROM excursion_alert WHERE excursion_id = ? AND triaged_at IS NOT NULL",
                excursion) == 1);
        return alertOf(excursion);
    }

    private Map<String, Object> alertOf(UUID excursion) {
        return jdbc.queryForMap("SELECT * FROM excursion_alert WHERE excursion_id = ?", excursion);
    }

    private String inboxStatusOf(UUID event) {
        return jdbc.queryForObject("SELECT status FROM inbox_event WHERE event_id = ?", String.class, event);
    }

    private int count(String sql, UUID id) {
        Integer counted = jdbc.queryForObject(sql, Integer.class, id);
        return counted == null ? 0 : counted;
    }

    private List<ConsumerRecord<String, String>> deadLetterUntil(String value) {
        List<ConsumerRecord<String, String>> seen = new ArrayList<>();
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            List<TopicPartition> partitions = consumer.partitionsFor(DEAD_LETTER_TOPIC).stream()
                    .map(info -> new TopicPartition(DEAD_LETTER_TOPIC, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            await().atMost(PATIENCE).until(() -> {
                consumer.poll(Duration.ofMillis(250)).forEach(seen::add);
                return seen.stream().anyMatch(record -> value.equals(record.value()));
            });
        }
        return seen;
    }

    private static String opened(UUID event, UUID shipment, UUID excursion, String peak) {
        return """
                {"eventId":"%s","type":"ExcursionOpened","occurredAt":"%s","shipmentId":"%s",\
                "organizationId":"%s","excursionId":"%s","kind":"ABOVE_MAX","peakCelsius":%s,\
                "durationMinutes":null}""".formatted(event, OPENED, shipment, UUID.randomUUID(), excursion, peak);
    }

    private static String closed(UUID event, UUID shipment, UUID excursion, long minutes) {
        return """
                {"eventId":"%s","type":"ExcursionClosed","occurredAt":"%s","shipmentId":"%s",\
                "organizationId":"%s","excursionId":"%s","kind":"ABOVE_MAX","peakCelsius":null,\
                "durationMinutes":%d}""".formatted(event, CLOSED, shipment, UUID.randomUUID(), excursion, minutes);
    }
}
