package com.coldchain.modules.shipment.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.modules.shipment.api.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShipmentTest {

    private static final UUID SHIPPER = UUID.fromString("01930000-0000-7000-8000-000000000001");

    private static final UUID CARRIER = UUID.fromString("01930000-0000-7000-8000-000000000002");

    private static final UUID HOSPITAL = UUID.fromString("01930000-0000-7000-8000-000000000003");

    private static final UUID ORIGIN = UUID.fromString("01930000-0000-7000-8000-000000000004");

    private static final UUID DESTINATION = UUID.fromString("01930000-0000-7000-8000-000000000005");

    private static final UUID DEVICE = UUID.fromString("01930000-0000-7000-8000-000000000006");

    private static final UUID PRODUCT = UUID.fromString("01930000-0000-7000-8000-000000000007");

    private static final FrozenThresholds FRIDGE = new FrozenThresholds(new BigDecimal("2.00"),
            new BigDecimal("8.00"), 30, 120, new BigDecimal("80.00"));

    private static final Instant DEPARTURE = Instant.parse("2026-04-01T08:00:00Z");

    @Test
    void aNewShipmentIsADraftHeldByWhoeverCreatedIt() {
        Shipment draft = draft();

        assertThat(draft.status()).isEqualTo(ShipmentStatus.DRAFT);
        assertThat(draft.currentCustodianOrganizationId())
                .describedAs("the box is in the shipper's own warehouse until it leaves")
                .isEqualTo(SHIPPER);
        assertThat(draft.thresholds())
                .describedAs("nothing is promised until it is dispatched")
                .isNull();
        assertThat(draft.dispatchedAt()).isNull();
        assertThat(draft.closedAt()).isNull();
    }

    @Test
    void dispatchFreezesWhatTheJourneyWillBeJudgedAgainst() {
        Shipment moving = loaded().dispatch(DEVICE, FRIDGE, DEPARTURE);

        assertThat(moving.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(moving.thresholds())
                .describedAs("the profile can be edited afterwards and this shipment is still "
                        + "judged against what was promised when it left")
                .isEqualTo(FRIDGE);
        assertThat(moving.deviceId()).isEqualTo(DEVICE);
        assertThat(moving.dispatchedAt()).isEqualTo(DEPARTURE);
    }

    @Test
    void aShipmentWithNothingInsideCannotLeave() {
        assertThatThrownBy(() -> draft().dispatch(DEVICE, FRIDGE, DEPARTURE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("carries nothing");
    }

    @Test
    void aShipmentWithNoSensorCannotLeave() {
        assertThatThrownBy(() -> loaded().dispatch(null, FRIDGE, DEPARTURE))
                .describedAs("a journey nobody measures cannot be certified at the end of it")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be monitored");
    }

    @Test
    void custodyMovesOnlyWhileTheShipmentIsOnItsWay() {
        Shipment moving = loaded().dispatch(DEVICE, FRIDGE, DEPARTURE);

        assertThat(moving.handOverTo(CARRIER).currentCustodianOrganizationId()).isEqualTo(CARRIER);
        assertThatThrownBy(() -> draft().handOverTo(CARRIER))
                .describedAs("handing over a box that never left is not custody, it is paperwork")
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> delivered().handOverTo(CARRIER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deliveryClosesTheJourneyAndLeavesItWithTheConsignee() {
        Shipment closed = delivered();

        assertThat(closed.status()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(closed.currentCustodianOrganizationId())
                .describedAs("whoever was driving it, the hospital is holding it now")
                .isEqualTo(HOSPITAL);
        assertThat(closed.closedAt()).isEqualTo(DEPARTURE.plusSeconds(7200));
    }

    @Test
    void anEndingCannotBeReopened() {
        Shipment closed = delivered();

        assertThatThrownBy(closed::arrive).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> closed.deliver(DEPARTURE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> closed.cancel(DEPARTURE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> closed.dispatch(DEVICE, FRIDGE, DEPARTURE))
                .describedAs("a delivered shipment that can be dispatched again is a certificate "
                        + "that means nothing")
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aDraftIsCancelledAndAJourneyIsRejected() {
        assertThat(loaded().cancel(DEPARTURE).status()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThatThrownBy(() -> loaded().dispatch(DEVICE, FRIDGE, DEPARTURE).cancel(DEPARTURE))
                .describedAs("what is already on a truck is refused at the door, not cancelled")
                .isInstanceOf(IllegalStateException.class);

        Shipment refused = loaded().dispatch(DEVICE, FRIDGE, DEPARTURE).arrive()
                .reject(DEPARTURE.plusSeconds(7200));

        assertThat(refused.status()).isEqualTo(ShipmentStatus.REJECTED);
        assertThat(refused.closedAt()).isNotNull();
    }

    @Test
    void everyChangeLeavesTheOriginalAlone() {
        Shipment draft = loaded();
        Shipment moving = draft.dispatch(DEVICE, FRIDGE, DEPARTURE);

        assertThat(draft.status())
                .describedAs("an aggregate that mutates in place cannot be reasoned about by "
                        + "whoever is holding the previous value")
                .isEqualTo(ShipmentStatus.DRAFT);
        assertThat(draft.deviceId()).isNull();
        assertThat(moving).isNotSameAs(draft);
        assertThat(moving.id()).isEqualTo(draft.id());
    }

    private Shipment draft() {
        return Shipment.createDraft(SHIPPER, "SHP-0001", ORIGIN, DESTINATION, HOSPITAL);
    }

    private Shipment loaded() {
        Shipment draft = draft();
        return draft.withLines(List.of(ShipmentLine.createNew(draft.id(), PRODUCT,
                "Influenza vaccine", 1, new BigDecimal("400.000"), "VIAL")));
    }

    private Shipment delivered() {
        return loaded().dispatch(DEVICE, FRIDGE, DEPARTURE).handOverTo(CARRIER).arrive()
                .deliver(DEPARTURE.plusSeconds(7200));
    }
}
