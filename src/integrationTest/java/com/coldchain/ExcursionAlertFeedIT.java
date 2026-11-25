package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.coldchain.modules.advisory.api.AdvisoryApi;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.shared.exception.DomainException;
import com.coldchain.shared.exception.ErrorCategory;
import com.coldchain.shared.util.RawUuid;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;

@IntegrationTest
class ExcursionAlertFeedIT {

    private static final String TOPIC = "coldchain.triage.alert.v1";

    private static final String DEAD_LETTER_TOPIC = "coldchain.advisory.alert-dlt.v1";

    private static final Duration PATIENCE = Duration.ofSeconds(30);

    private static final Instant OPENED = Instant.parse("2026-05-01T10:05:00Z");

    private static final Instant CLOSED = Instant.parse("2026-05-01T10:50:00Z");

    @Autowired
    private AdvisoryApi advisory;

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private KafkaTemplate<String, byte[]> kafka;

    @Autowired
    private KafkaContainer broker;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anAlertPublishedByTriageIsStoredForItsShipment() {
        RegisterOrganizationResult owner = fixtures.organization("advised");
        ShipmentResult shipment = fixtures.draftShipment(owner);
        UUID excursion = UUID.randomUUID();

        publish(shipment.id(), modelAlert(excursion, shipment.id(), owner.organizationId(), CLOSED));

        ExcursionAlertResult alert = awaitAlert(shipment.id(), owner.organizationId(), excursion);
        assertThat(alert.source()).isEqualTo(AlertSource.LLM);
        assertThat(alert.probableCause()).isEqualTo(ProbableCause.DOOR_OPENING);
        assertThat(alert.confidence()).isEqualByComparingTo("0.80");
        assertThat(alert.model()).isEqualTo("claude-opus-5-5");
        assertThat(alert.summaryPt()).isEqualTo("Um pico curto que se recuperou sozinho.");
        assertThat(alert.contextAvailable()).isTrue();
        assertThat(alert.triagedAt()).isEqualTo(CLOSED);
        assertThat(createdByOf(excursion))
                .describedAs("no user wrote the alert, so the system did")
                .isEqualTo(new UUID(0, 0));
    }

    @Test
    void theSameAlertTwiceIsOneRow() {
        RegisterOrganizationResult owner = fixtures.organization("twice");
        ShipmentResult shipment = fixtures.draftShipment(owner);
        UUID excursion = UUID.randomUUID();
        UUID marker = UUID.randomUUID();
        String once = rulesAlert(excursion, shipment.id(), owner.organizationId(), OPENED);

        publish(shipment.id(), once);
        publish(shipment.id(), once);
        publish(shipment.id(), rulesAlert(marker, shipment.id(), owner.organizationId(), OPENED));

        awaitAlert(shipment.id(), owner.organizationId(), marker);
        assertThat(rowsOf(excursion))
                .describedAs("Kafka delivers at least once, and the newest-wins rule makes it one alert")
                .isEqualTo(1);
    }

    @Test
    void anOlderTriageArrivingAfterANewerOneLeavesTheNewer() {
        RegisterOrganizationResult owner = fixtures.organization("reordered");
        ShipmentResult shipment = fixtures.draftShipment(owner);
        UUID excursion = UUID.randomUUID();
        UUID marker = UUID.randomUUID();

        publish(shipment.id(), modelAlert(excursion, shipment.id(), owner.organizationId(), CLOSED));
        publish(shipment.id(), rulesAlert(excursion, shipment.id(), owner.organizationId(), OPENED));
        publish(shipment.id(), rulesAlert(marker, shipment.id(), owner.organizationId(), OPENED));

        awaitAlert(shipment.id(), owner.organizationId(), marker);
        ExcursionAlertResult kept = alertOf(shipment.id(), owner.organizationId(), excursion);
        assertThat(kept.source())
                .describedAs("the open's triage, redelivered late, cannot overwrite the close's")
                .isEqualTo(AlertSource.LLM);
        assertThat(kept.triagedAt()).isEqualTo(CLOSED);
    }

    @Test
    void anOrganizationThatTakesNoPartCannotReadTheAdvice() {
        RegisterOrganizationResult owner = fixtures.organization("private");
        RegisterOrganizationResult stranger = fixtures.organization("stranger");
        ShipmentResult shipment = fixtures.draftShipment(owner);
        UUID excursion = UUID.randomUUID();

        publish(shipment.id(), rulesAlert(excursion, shipment.id(), owner.organizationId(), OPENED));
        awaitAlert(shipment.id(), owner.organizationId(), excursion);

        assertThatThrownBy(() -> advisory.alertsOf(shipment.id(), stranger.organizationId()))
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().category())
                .describedAs("the advice about a shipment is as invisible as the shipment: not found, "
                        + "never forbidden")
                .isEqualTo(ErrorCategory.NOT_FOUND);
    }

    @Test
    void aMalformedAlertIsDeadLetteredAndTheNextOneStillArrives() {
        RegisterOrganizationResult owner = fixtures.organization("poisoned");
        ShipmentResult shipment = fixtures.draftShipment(owner);
        UUID excursion = UUID.randomUUID();
        String poison = "{not an alert " + UUID.randomUUID();
        String unknownCause = rulesAlert(UUID.randomUUID(), shipment.id(), owner.organizationId(), OPENED)
                .replace("\"UNKNOWN\"", "\"COMPRESSOR\"");

        publish(shipment.id(), poison);
        publish(shipment.id(), unknownCause);
        publish(shipment.id(), rulesAlert(excursion, shipment.id(), owner.organizationId(), OPENED));

        awaitAlert(shipment.id(), owner.organizationId(), excursion);
        List<String> deadLettered = deadLetteredUntil(values -> values.contains(poison)
                && values.contains(unknownCause));
        assertThat(deadLettered)
                .describedAs("a message that can never be read leaves the partition instead of blocking it, "
                        + "with the bytes it arrived with")
                .contains(poison, unknownCause);
    }

    private void publish(UUID shipment, String payload) {
        try {
            kafka.send(TOPIC, shipment.toString(), payload.getBytes(StandardCharsets.UTF_8))
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception cause) {
            throw new IllegalStateException("Could not publish the alert", cause);
        }
    }

    private ExcursionAlertResult awaitAlert(UUID shipment, UUID viewer, UUID excursion) {
        await().atMost(PATIENCE).until(() -> advisory.alertsOf(shipment, viewer).stream()
                .anyMatch(alert -> alert.excursionId().equals(excursion)));
        return alertOf(shipment, viewer, excursion);
    }

    private ExcursionAlertResult alertOf(UUID shipment, UUID viewer, UUID excursion) {
        return advisory.alertsOf(shipment, viewer).stream()
                .filter(alert -> alert.excursionId().equals(excursion))
                .findFirst()
                .orElseThrow();
    }

    private int rowsOf(UUID excursion) {
        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM EXCURSION_ALERT WHERE EXCURSION_ID = ?",
                Integer.class, (Object) RawUuid.toBytes(excursion));
        return rows == null ? 0 : rows;
    }

    private UUID createdByOf(UUID excursion) {
        byte[] createdBy = jdbc.queryForObject("SELECT CREATED_BY FROM EXCURSION_ALERT WHERE EXCURSION_ID = ?",
                byte[].class, (Object) RawUuid.toBytes(excursion));
        return RawUuid.fromBytes(createdBy);
    }

    private List<String> deadLetteredUntil(Predicate<List<String>> done) {
        List<String> seen = new ArrayList<>();
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            await().atMost(PATIENCE).until(() -> !consumer.partitionsFor(DEAD_LETTER_TOPIC).isEmpty());
            List<TopicPartition> partitions = consumer.partitionsFor(DEAD_LETTER_TOPIC).stream()
                    .map(info -> new TopicPartition(DEAD_LETTER_TOPIC, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            await().atMost(PATIENCE).until(() -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(250))) {
                    seen.add(record.value());
                }
                return done.test(seen);
            });
        }
        return seen;
    }

    private static String modelAlert(UUID excursion, UUID shipment, UUID organization, Instant triagedAt) {
        return """
                {"excursionId":"%s","shipmentId":"%s","organizationId":"%s","probableCause":"DOOR_OPENING",\
                "severity":"LOW","confidence":0.8,"summaryEn":"A short spike that recovered on its own.",\
                "summaryEs":"Un pico corto que se recuperó solo.","summaryPt":"Um pico curto que se recuperou sozinho.",\
                "recommendedAction":"Keep the doors shut while loading.","source":"LLM","model":"claude-opus-5-5",\
                "contextAvailable":true,"triagedAt":"%s","promptVersion":"triage-v1"}"""
                .formatted(excursion, shipment, organization, triagedAt);
    }

    private static String rulesAlert(UUID excursion, UUID shipment, UUID organization, Instant triagedAt) {
        return """
                {"excursionId":"%s","shipmentId":"%s","organizationId":"%s","probableCause":"UNKNOWN",\
                "severity":"MEDIUM","confidence":null,"summaryEn":"The temperature went above the maximum.",\
                "summaryEs":"La temperatura superó el máximo.","summaryPt":"A temperatura passou do máximo.",\
                "recommendedAction":"Check the shipment now.","source":"RULES","model":null,\
                "contextAvailable":false,"triagedAt":"%s"}"""
                .formatted(excursion, shipment, organization, triagedAt);
    }
}
