package com.coldchain.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ExcursionFeedIT {

    private static final String DEAD_LETTER_TOPIC = "coldchain.triage.excursion-dlt.v1";

    private static final String ALERT_TOPIC = "coldchain.triage.alert.v1";

    private static final Duration PATIENCE = Duration.ofSeconds(30);

    private static final Instant OPENED = Instant.parse("2026-05-01T10:05:00Z");

    private static final Instant CLOSED = Instant.parse("2026-05-01T10:50:00Z");

    private static final String DOOR_OPENING = """
            {"probableCause":"DOOR_OPENING","severity":"LOW","confidence":0.8,\
            "summary":{"en":"A short spike that recovered on its own.","es":"Un pico corto que se recuperó solo.",\
            "pt":"Um pico curto que se recuperou sozinho."},\
            "recommendedAction":"Keep the doors shut while loading."}""";

    @Autowired
    private KafkaTemplate<String, byte[]> kafka;

    @Autowired
    private KafkaContainer broker;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FakeColdChainApi api;

    @Autowired
    private FakeChatModel model;

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
    void aClosedExcursionIsWeighedAgainstWhatItsShipmentAllows() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID closing = UUID.randomUUID();
        api.knows(shipment, 60);

        publish(shipment, opened(UUID.randomUUID(), shipment, excursion, "11.50"));
        publish(shipment, closed(closing, shipment, excursion, 45));

        awaitProcessed(closing);
        Map<String, Object> alert = alertOf(excursion);
        assertThat(alert.get("status")).isEqualTo("CLOSED");
        assertThat(alert.get("context_available")).isEqualTo(true);
        assertThat(alert.get("severity"))
                .describedAs("forty-five minutes is a warning on a shipment that tolerates an hour out of range")
                .isEqualTo("MEDIUM");
    }

    @Test
    void withTheApiDownTheAlertStillGoesOutWithoutContext() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID closing = UUID.randomUUID();
        api.isDownFor(shipment);

        publish(shipment, closed(closing, shipment, excursion, 45));

        awaitProcessed(closing);
        Map<String, Object> alert = alertOf(excursion);
        assertThat(alert.get("triaged_at")).isNotNull();
        assertThat(alert.get("source")).isEqualTo("RULES");
        assertThat(alert.get("context_available"))
                .describedAs("an API that does not answer degrades the alert; it never withholds it")
                .isEqualTo(false);
        assertThat(alert.get("severity")).isEqualTo("HIGH");
    }

    @Test
    void aClosedExcursionWithItsContextIsTriagedByTheModel() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID closing = UUID.randomUUID();
        api.knows(shipment, 60);
        model.willAnswer(DOOR_OPENING);

        publish(shipment, opened(UUID.randomUUID(), shipment, excursion, "11.50"));
        publish(shipment, closed(closing, shipment, excursion, 45));

        awaitProcessed(closing);
        Map<String, Object> alert = alertOf(excursion);
        assertThat(alert.get("source")).isEqualTo("LLM");
        assertThat(alert.get("probable_cause")).isEqualTo("DOOR_OPENING");
        assertThat(alert.get("severity")).isEqualTo("LOW");
        assertThat(alert.get("confidence")).asString().isEqualTo("0.80");
        assertThat(alert.get("summary_es")).isEqualTo("Un pico corto que se recuperó solo.");
        assertThat(alert.get("model")).isEqualTo(FakeChatModel.MODEL);
        assertThat(alert.get("prompt_version")).isEqualTo("triage-v1");
        assertThat(alert.get("input_tokens")).isEqualTo(FakeChatModel.INPUT_TOKENS);
        assertThat(alert.get("output_tokens")).isEqualTo(FakeChatModel.OUTPUT_TOKENS);
        assertThat(alert.get("latency_ms")).isNotNull();
        assertThat(alert.get("context_available")).isEqualTo(true);
    }

    @Test
    void aModelThatDoesNotAnswerLeavesTheAlertToTheRules() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID closing = UUID.randomUUID();
        api.knows(shipment, 60);
        int askedBefore = model.timesAsked();

        publish(shipment, opened(UUID.randomUUID(), shipment, excursion, "11.50"));
        publish(shipment, closed(closing, shipment, excursion, 45));

        awaitProcessed(closing);
        Map<String, Object> alert = alertOf(excursion);
        assertThat(model.timesAsked()).isGreaterThan(askedBefore);
        assertThat(alert.get("source"))
                .describedAs("a model that fails degrades the alert to the rules; it never withholds it")
                .isEqualTo("RULES");
        assertThat(alert.get("model")).isNull();
        assertThat(alert.get("severity")).isEqualTo("MEDIUM");
        assertThat(alert.get("context_available")).isEqualTo(true);
    }

    @Test
    void anAnswerOutsideTheSchemaIsDiscardedAndTheRulesAnswer() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID closing = UUID.randomUUID();
        api.knows(shipment, 60);
        model.willAnswer(DOOR_OPENING.replace("0.8", "1.2"));

        publish(shipment, opened(UUID.randomUUID(), shipment, excursion, "11.50"));
        publish(shipment, closed(closing, shipment, excursion, 45));

        awaitProcessed(closing);
        Map<String, Object> alert = alertOf(excursion);
        assertThat(alert.get("source")).isEqualTo("RULES");
        assertThat(alert.get("confidence")).isNull();
        assertThat(inboxStatusOf(closing))
                .describedAs("a bad answer is the model's failure, never a reason to retry or dead-letter the event")
                .isEqualTo("PROCESSED");
    }

    @Test
    void withoutTheContextTheModelIsNotAsked() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID closing = UUID.randomUUID();
        api.isDownFor(shipment);
        int askedBefore = model.timesAsked();

        publish(shipment, closed(closing, shipment, excursion, 45));

        awaitProcessed(closing);
        assertThat(model.timesAsked())
                .describedAs("without the readings there is nothing for the model to read")
                .isEqualTo(askedBefore);
        assertThat(alertOf(excursion).get("source")).isEqualTo("RULES");
    }

    @Test
    void everyTriageLeavesForTheApiKeyedByItsShipment() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        UUID closing = UUID.randomUUID();
        api.knows(shipment, 60);
        model.willAnswer(DOOR_OPENING);

        publish(shipment, opened(UUID.randomUUID(), shipment, excursion, "11.50"));
        publish(shipment, closed(closing, shipment, excursion, 45));

        awaitProcessed(closing);
        List<ConsumerRecord<String, String>> published = readUntil(ALERT_TOPIC,
                value -> value.contains(excursion.toString()) && value.contains("\"source\":\"LLM\""));
        List<ConsumerRecord<String, String>> ofThisExcursion = published.stream()
                .filter(record -> record.value().contains(excursion.toString()))
                .toList();
        assertThat(ofThisExcursion)
                .describedAs("the rules' alert at the open, then the model's at the close, in that order")
                .extracting(record -> record.value().contains("\"source\":\"RULES\"") ? "RULES" : "LLM")
                .containsExactly("RULES", "LLM");
        assertThat(ofThisExcursion).allSatisfy(record -> assertThat(record.key()).isEqualTo(shipment.toString()));
        assertThat(ofThisExcursion.getLast().value())
                .contains("\"probableCause\":\"DOOR_OPENING\"", "\"summaryPt\":\"Um pico curto que se recuperou sozinho.\"",
                        "\"model\":\"" + FakeChatModel.MODEL + "\"", "\"contextAvailable\":true");
    }

    @Test
    void aMalformedMessageIsDeadLetteredAndTheNextOneStillArrives() {
        UUID shipment = UUID.randomUUID();
        UUID excursion = UUID.randomUUID();
        String poison = "{not json " + UUID.randomUUID();

        publish(shipment, poison);
        publish(shipment, opened(UUID.randomUUID(), shipment, excursion, "9.50"));

        awaitAlert(excursion);
        List<ConsumerRecord<String, String>> deadLettered = readUntil(DEAD_LETTER_TOPIC, poison::equals);
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
            kafka.send(TestcontainersConfiguration.FEED_TOPIC, shipment.toString(),
                    payload.getBytes(StandardCharsets.UTF_8)).get(10, TimeUnit.SECONDS);
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

    private void awaitProcessed(UUID event) {
        await().atMost(PATIENCE).until(() -> count(
                "SELECT count(*) FROM inbox_event WHERE event_id = ? AND status = 'PROCESSED'", event) == 1);
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

    private List<ConsumerRecord<String, String>> readUntil(String topic, Predicate<String> wanted) {
        List<ConsumerRecord<String, String>> seen = new ArrayList<>();
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            await().atMost(PATIENCE).until(() -> !consumer.partitionsFor(topic).isEmpty());
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            await().atMost(PATIENCE).until(() -> {
                consumer.poll(Duration.ofMillis(250)).forEach(seen::add);
                return seen.stream().anyMatch(record -> wanted.test(record.value()));
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
