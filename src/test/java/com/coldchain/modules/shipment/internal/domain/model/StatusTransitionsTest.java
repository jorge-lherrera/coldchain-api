package com.coldchain.modules.shipment.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.shipment.api.ShipmentStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StatusTransitionsTest {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> THE_WHOLE_TABLE = Map.of(
            ShipmentStatus.DRAFT, Set.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.CANCELLED),
            ShipmentStatus.IN_TRANSIT, Set.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.AT_DESTINATION),
            ShipmentStatus.AT_DESTINATION, Set.of(ShipmentStatus.DELIVERED, ShipmentStatus.REJECTED),
            ShipmentStatus.DELIVERED, Set.of(),
            ShipmentStatus.REJECTED, Set.of(),
            ShipmentStatus.CANCELLED, Set.of());

    @Test
    void everyPairOfStatusesIsEitherAllowedOrRefused() {
        for (ShipmentStatus from : ShipmentStatus.values()) {
            for (ShipmentStatus to : ShipmentStatus.values()) {
                assertThat(StatusTransitions.allows(from, to))
                        .describedAs("%s -> %s", from, to)
                        .isEqualTo(THE_WHOLE_TABLE.get(from).contains(to));
            }
        }
    }

    @Test
    void aDeliveredShipmentGoesNowhere() {
        List<ShipmentStatus> endings = List.of(ShipmentStatus.DELIVERED, ShipmentStatus.REJECTED,
                ShipmentStatus.CANCELLED);

        assertThat(endings).allSatisfy(ending -> {
            assertThat(StatusTransitions.terminal(ending))
                    .describedAs("%s is an ending, and an ending that can be reopened is not one",
                            ending)
                    .isTrue();
            assertThat(ShipmentStatus.values()).allSatisfy(target ->
                    assertThat(StatusTransitions.allows(ending, target)).isFalse());
        });
    }

    @Test
    void aShipmentInTransitStaysInTransitWhileCustodyMoves() {
        assertThat(StatusTransitions.allows(ShipmentStatus.IN_TRANSIT, ShipmentStatus.IN_TRANSIT))
                .describedAs("a handoff changes who holds the box, not what the box is doing")
                .isTrue();
        assertThat(StatusTransitions.terminal(ShipmentStatus.IN_TRANSIT)).isFalse();
    }

    @Test
    void nothingGoesBackToDraftAndNothingSkipsTheJourney() {
        assertThat(ShipmentStatus.values()).allSatisfy(from ->
                assertThat(StatusTransitions.allows(from, ShipmentStatus.DRAFT))
                        .describedAs("%s returns to DRAFT, which would let a dispatched shipment be "
                                + "edited as if it had never left", from)
                        .isFalse());
        assertThat(StatusTransitions.allows(ShipmentStatus.DRAFT, ShipmentStatus.DELIVERED))
                .describedAs("a shipment delivered without ever being dispatched has no journey to "
                        + "certify")
                .isFalse();
        assertThat(StatusTransitions.allows(ShipmentStatus.IN_TRANSIT, ShipmentStatus.CANCELLED))
                .describedAs("cancelling what is already on a truck is a rejection, not a "
                        + "cancellation")
                .isFalse();
    }
}
