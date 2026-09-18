package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.telemetry.api.BatchStatus;
import com.coldchain.modules.telemetry.api.DiscardReason;
import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.api.ExcursionStatus;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.AssignDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.DiscardedReading;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.IngestBatchResult;
import com.coldchain.modules.telemetry.api.dto.MonitoringThresholds;
import com.coldchain.modules.telemetry.api.dto.ReadingCommand;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.SeriesResult;
import com.coldchain.shared.identifier.RawUuid;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class TelemetryIngestionIT {

    private static final MonitoringThresholds FRIDGE =
            new MonitoringThresholds(new BigDecimal("2.00"), new BigDecimal("8.00"));

    private static final Instant START = Instant.parse("2026-03-01T10:00:00Z");

    @Autowired
    private TelemetryApi telemetry;

    @Autowired
    private IdentityApi identity;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void resendingABatchWritesNothing() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);
        IngestBatchCommand batch = new IngestBatchCommand(organization, device.id(), key(),
                List.of(reading(0, "4.00"), reading(5, "4.20"), reading(10, "4.10")));

        IngestBatchResult first = telemetry.ingestBatch(batch);
        IngestBatchResult again = telemetry.ingestBatch(batch);

        assertThat(first.status()).isEqualTo(BatchStatus.ACCEPTED);
        assertThat(first.acceptedCount()).isEqualTo(3);
        assertThat(again.status())
                .describedAs("a gateway with bad signal resends, and resending must be free")
                .isEqualTo(BatchStatus.REPLAYED);
        assertThat(again.batchId()).isEqualTo(first.batchId());
        assertThat(countReadings(shipment)).isEqualTo(3);
    }

    @Test
    void aReadingOutsideEveryWindowIsDiscardedWithItsReason() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);

        IngestBatchResult result = telemetry.ingestBatch(new IngestBatchCommand(organization,
                device.id(), key(), List.of(
                        reading(0, "4.00"),
                        new ReadingCommand(START.minus(2, ChronoUnit.DAYS), new BigDecimal("4.00")),
                        new ReadingCommand(START.plus(30, ChronoUnit.MINUTES), new BigDecimal("300.00")))));

        assertThat(result.status()).isEqualTo(BatchStatus.PARTIAL);
        assertThat(result.acceptedCount()).isEqualTo(1);
        assertThat(result.discarded()).extracting(DiscardedReading::reason)
                .describedAs("a reading nobody can place is not hung off the current shipment")
                .containsExactlyInAnyOrder(DiscardReason.OUT_OF_WINDOW, DiscardReason.IMPOSSIBLE_VALUE);
    }

    @Test
    void anExcursionOpensOutOfBandAndClosesWhenItComesBack() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);

        telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(), List.of(
                reading(0, "4.00"),
                reading(5, "9.50"),
                reading(10, "10.20"),
                reading(15, "4.10"))));

        SeriesResult series = telemetry.seriesOf(shipment, organization);

        assertThat(series.points()).hasSize(4);
        assertThat(series.excursions()).hasSize(1);
        assertThat(series.excursions().getFirst().kind()).isEqualTo(ExcursionKind.ABOVE_MAX);
        assertThat(series.excursions().getFirst().status()).isEqualTo(ExcursionStatus.CLOSED);
        assertThat(series.excursions().getFirst().peakCelsius()).isEqualByComparingTo("10.20");
        assertThat(series.excursions().getFirst().durationMinutes())
                .describedAs("it opens with the first reading out of band and closes with the "
                        + "first one back inside")
                .isEqualTo(10);
    }

    @Test
    void anExcursionStaysOpenWhileTheSeriesEndsOutOfBand() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);

        telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(), List.of(
                reading(0, "4.00"),
                reading(5, "1.20"))));

        SeriesResult series = telemetry.seriesOf(shipment, organization);

        assertThat(series.excursions()).hasSize(1);
        assertThat(series.excursions().getFirst().kind()).isEqualTo(ExcursionKind.BELOW_MIN);
        assertThat(series.excursions().getFirst().status()).isEqualTo(ExcursionStatus.OPEN);
    }

    @Test
    void aBatchThatArrivesOutOfOrderIsStillReadInOrder() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);

        telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(), List.of(
                reading(15, "4.10"),
                reading(0, "4.00"),
                reading(10, "10.20"),
                reading(5, "9.50"))));

        SeriesResult series = telemetry.seriesOf(shipment, organization);

        assertThat(series.points()).extracting(point -> point.measuredAt().toString())
                .describedAs("order is never assumed on the way in and always applied on the way out")
                .isSorted();
        assertThat(series.excursions()).hasSize(1);
    }

    @Test
    void theSameSampleTwiceIsADuplicate() {
        UUID organization = organization();
        UUID shipment = UUID.randomUUID();
        DeviceResult device = assignedDevice(organization, shipment);

        telemetry.ingestBatch(new IngestBatchCommand(organization, device.id(), key(),
                List.of(reading(0, "4.00"))));
        IngestBatchResult second = telemetry.ingestBatch(new IngestBatchCommand(organization,
                device.id(), key(), List.of(reading(0, "4.50"), reading(5, "4.20"))));

        assertThat(second.discarded()).extracting(DiscardedReading::reason)
                .containsExactly(DiscardReason.DUPLICATE);
        assertThat(countReadings(shipment)).isEqualTo(2);
    }

    private int countReadings(UUID shipmentId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM temperature_reading WHERE shipment_id = ?",
                Integer.class, RawUuid.toBytes(shipmentId));
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
                "TAX-" + suffix, "Telemetry Labs S.A.", "Telemetry", OrganizationKind.LAB, "UY",
                "admin-" + suffix + "@telemetry.test", "Telemetry Admin", "Telemetry!Secret42"))
                .organizationId();
    }
}
