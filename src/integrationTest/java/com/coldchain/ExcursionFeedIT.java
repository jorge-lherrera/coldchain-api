package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.AssignDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.MonitoringThresholds;
import com.coldchain.modules.telemetry.api.dto.ReadingCommand;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
class ExcursionFeedIT {

    private static final String TOPIC = "coldchain.telemetry.excursion.v1";

    private static final MonitoringThresholds FRIDGE =
            new MonitoringThresholds(new BigDecimal("2.00"), new BigDecimal("8.00"));

    private static final Instant START = Instant.parse("2026-05-01T10:00:00Z");

    private static final int EXCURSIONS_IN_ONE_BATCH = 4;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private TelemetryApi telemetry;

    @Autowired
    private IdentityApi identity;

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private PlatformTransactionManager transactions;

    @Test
    void anOpenedExcursionReachesTheFeedKeyedByItsShipment() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);

        telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(),
                List.of(reading(0, "4.00"), reading(5, "9.50"))));

        List<ConsumerRecord<String, String>> feed = feedUntil(records -> keyed(records, shipment).size() == 1);
        ConsumerRecord<String, String> record = keyed(feed, shipment).getFirst();
        JsonNode message = JSON.readTree(record.value());

        assertThat(record.key())
                .describedAs("the key is what keeps every event of a shipment on one partition")
                .isEqualTo(shipment.toString());
        assertThat(message.get("type").asString()).isEqualTo("ExcursionOpened");
        assertThat(message.get("eventId").asString()).isNotBlank();
        assertThat(message.get("shipmentId").asString()).isEqualTo(shipment.toString());
        assertThat(message.get("organizationId").asString()).isEqualTo(organization.toString());
        assertThat(message.get("excursionId").asString()).isNotBlank();
        assertThat(message.get("kind").asString()).isEqualTo("ABOVE_MAX");
        assertThat(message.get("peakCelsius").decimalValue()).isEqualByComparingTo("9.50");
        assertThat(Instant.parse(message.get("occurredAt").asString()))
                .isEqualTo(START.plus(5, ChronoUnit.MINUTES));
    }

    @Test
    void theEventsOfOneBatchArriveInTheOrderTheyHappenedOnOnePartition() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);

        List<ReadingCommand> readings = new ArrayList<>();
        for (int excursion = 0; excursion < EXCURSIONS_IN_ONE_BATCH; excursion++) {
            readings.add(reading(excursion * 10, "4.00"));
            readings.add(reading(excursion * 10 + 5, "9.50"));
        }
        readings.add(reading(EXCURSIONS_IN_ONE_BATCH * 10, "4.00"));
        telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(), readings));

        List<ConsumerRecord<String, String>> events = keyed(
                feedUntil(records -> keyed(records, shipment).size() == 2 * EXCURSIONS_IN_ONE_BATCH), shipment);
        List<JsonNode> messages = events.stream().map(record -> JSON.readTree(record.value())).toList();

        assertThat(events).extracting(ConsumerRecord::partition).containsOnly(events.getFirst().partition());
        assertThat(messages).extracting(message -> Instant.parse(message.get("occurredAt").asString()))
                .describedAs("one commit hands every event of the batch over at once, and they must reach "
                        + "the partition in the order they happened, not in the order their threads won")
                .isSorted();
        for (int pair = 0; pair < EXCURSIONS_IN_ONE_BATCH; pair++) {
            JsonNode opened = messages.get(2 * pair);
            JsonNode closed = messages.get(2 * pair + 1);
            assertThat(List.of(opened.get("type").asString(), closed.get("type").asString()))
                    .describedAs("a close never overtakes its open on the same partition")
                    .containsExactly("ExcursionOpened", "ExcursionClosed");
            assertThat(closed.get("excursionId").asString()).isEqualTo(opened.get("excursionId").asString());
            assertThat(closed.get("durationMinutes").asLong()).isEqualTo(5);
        }
    }

    @Test
    void anExcursionClosedByALaterBatchKeepsTheIdItOpenedWith() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);

        telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(),
                List.of(reading(0, "4.00"), reading(5, "9.50"))));
        telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(),
                List.of(reading(10, "4.10"))));

        List<JsonNode> events = keyed(feedUntil(records -> keyed(records, shipment).size() == 2), shipment)
                .stream().map(record -> JSON.readTree(record.value())).toList();
        JsonNode opened = events.stream().filter(event -> event.get("type").asString().equals("ExcursionOpened"))
                .findFirst().orElseThrow();
        JsonNode closed = events.stream().filter(event -> event.get("type").asString().equals("ExcursionClosed"))
                .findFirst().orElseThrow();

        assertThat(closed.get("excursionId").asString())
                .describedAs("a consumer pairs the close with its open by excursionId, so the id cannot "
                        + "change between the batch that opened it and the one that closed it")
                .isEqualTo(opened.get("excursionId").asString());
    }

    @Test
    void anIngestionThatRollsBackSendsNothing() {
        UUID organization = organization();
        UUID rolledBack = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, rolledBack);

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(),
                    List.of(reading(0, "4.00"), reading(5, "9.50"))));
            status.setRollbackOnly();
        });

        UUID committed = UUID.randomUUID();
        DeviceResult other = assignedDevice(organization, committed);
        telemetry.ingestBatch(new IngestBatchCommand(organization, other.id(), key(),
                List.of(reading(0, "4.00"), reading(5, "9.50"))));

        List<ConsumerRecord<String, String>> feed = feedUntil(records -> keyed(records, committed).size() == 1);

        assertThat(keyed(feed, rolledBack))
                .describedAs("an excursion that was never committed never existed, and the feed must not "
                        + "announce it, even though the one committed after it already went out")
                .isEmpty();
    }

    private List<ConsumerRecord<String, String>> feedUntil(Predicate<List<ConsumerRecord<String, String>>> done) {
        List<ConsumerRecord<String, String>> seen = new ArrayList<>();
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            List<TopicPartition> partitions = consumer.partitionsFor(TOPIC).stream()
                    .map(info -> new TopicPartition(TOPIC, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                consumer.poll(Duration.ofMillis(250)).forEach(seen::add);
                return done.test(seen);
            });
        }
        return seen;
    }

    private static List<ConsumerRecord<String, String>> keyed(List<ConsumerRecord<String, String>> records,
            UUID shipmentId) {
        return records.stream().filter(record -> shipmentId.toString().equals(record.key())).toList();
    }

    private DeviceResult assignedDevice(UUID organization, UUID shipment) {
        DeviceResult device = telemetry.registerDevice(new RegisterDeviceCommand(organization,
                "SN-" + UUID.randomUUID().toString().substring(0, 8), "Tag-1", "1.4.2", 300,
                START.minus(30, ChronoUnit.DAYS)));
        telemetry.assignDevice(new AssignDeviceCommand(device.id(), shipment, FRIDGE, START));
        return device;
    }

    private static ReadingCommand reading(int minutesFromStart, String celsius) {
        return new ReadingCommand(START.plus(minutesFromStart, ChronoUnit.MINUTES),
                new BigDecimal(celsius));
    }

    private static String key() {
        return "batch-" + UUID.randomUUID();
    }

    private UUID organization() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, "Feed Labs S.A.", "Feed", OrganizationKind.LAB, "UY",
                "admin-" + suffix + "@feed.test", "Feed Admin", "Feed!Secret42"))
                .organizationId();
    }
}
