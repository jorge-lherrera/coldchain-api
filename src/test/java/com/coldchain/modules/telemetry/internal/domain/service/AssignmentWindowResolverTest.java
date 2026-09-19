package com.coldchain.modules.telemetry.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssignmentWindowResolverTest {

    private static final UUID DEVICE = UUID.fromString("01930000-0000-7000-8000-000000000001");

    private static final UUID FIRST_SHIPMENT = UUID.fromString("01930000-0000-7000-8000-000000000002");

    private static final UUID SECOND_SHIPMENT = UUID.fromString("01930000-0000-7000-8000-000000000003");

    private static final Instant MONDAY = Instant.parse("2026-04-06T08:00:00Z");

    private static final Instant TUESDAY = MONDAY.plus(1, ChronoUnit.DAYS);

    private static final Instant WEDNESDAY = MONDAY.plus(2, ChronoUnit.DAYS);

    @Test
    void aReadingBelongsToTheShipmentTheDeviceWasCarryingAtThatMoment() {
        List<DeviceAssignment> windows = List.of(
                closed(FIRST_SHIPMENT, MONDAY, TUESDAY),
                open(SECOND_SHIPMENT, WEDNESDAY));

        assertThat(AssignmentWindowResolver.resolve(windows, MONDAY.plus(6, ChronoUnit.HOURS)))
                .get()
                .extracting(DeviceAssignment::shipmentId)
                .isEqualTo(FIRST_SHIPMENT);
        assertThat(AssignmentWindowResolver.resolve(windows, WEDNESDAY.plus(6, ChronoUnit.HOURS)))
                .get()
                .extracting(DeviceAssignment::shipmentId)
                .isEqualTo(SECOND_SHIPMENT);
    }

    @Test
    void aReadingFromBetweenTwoJourneysBelongsToNeither() {
        List<DeviceAssignment> windows = List.of(
                closed(FIRST_SHIPMENT, MONDAY, TUESDAY),
                open(SECOND_SHIPMENT, WEDNESDAY));

        assertThat(AssignmentWindowResolver.resolve(windows, TUESDAY.plus(6, ChronoUnit.HOURS)))
                .describedAs("a sensor sitting in a drawer measures the drawer, and that is not "
                        + "evidence about anybody's shipment")
                .isEmpty();
    }

    @Test
    void theMomentItIsAttachedIsInsideTheWindowAndTheMomentItIsDetachedIsNot() {
        List<DeviceAssignment> windows = List.of(closed(FIRST_SHIPMENT, MONDAY, TUESDAY));

        assertThat(AssignmentWindowResolver.resolve(windows, MONDAY))
                .describedAs("the first reading of a journey is taken as it leaves")
                .isPresent();
        assertThat(AssignmentWindowResolver.resolve(windows, TUESDAY))
                .describedAs("the detachment closes the window, so the reading that arrives at that "
                        + "very instant is already outside it")
                .isEmpty();
    }

    @Test
    void aWindowThatWasNeverClosedRunsOn() {
        List<DeviceAssignment> windows = List.of(open(SECOND_SHIPMENT, MONDAY));

        assertThat(AssignmentWindowResolver.resolve(windows, MONDAY.plus(400, ChronoUnit.DAYS)))
                .isPresent();
        assertThat(AssignmentWindowResolver.resolve(windows, MONDAY.minusSeconds(1)))
                .describedAs("a reading from before the sensor was attached is not part of the "
                        + "journey either")
                .isEmpty();
    }

    @Test
    void aDeviceThatWasNeverAttachedToAnythingCoversNothing() {
        assertThat(AssignmentWindowResolver.resolve(List.of(), MONDAY)).isEmpty();
    }

    private DeviceAssignment open(UUID shipmentId, Instant attachedAt) {
        return DeviceAssignment.attach(DEVICE, shipmentId, new BigDecimal("2.00"),
                new BigDecimal("8.00"), attachedAt);
    }

    private DeviceAssignment closed(UUID shipmentId, Instant attachedAt, Instant detachedAt) {
        return DeviceAssignment.restore(UUID.randomUUID(), DEVICE, shipmentId,
                new BigDecimal("2.00"), new BigDecimal("8.00"), attachedAt, detachedAt, 0);
    }
}
