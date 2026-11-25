package com.coldchain.modules.shipment.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.api.Participation;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustodyHistoryTest {

    private static final UUID SHIPMENT = UUID.fromString("01930000-0000-7000-8000-000000000001");

    private static final UUID LABORATORY = UUID.fromString("01930000-0000-7000-8000-000000000002");

    private static final UUID CARRIER = UUID.fromString("01930000-0000-7000-8000-000000000003");

    private static final UUID HOSPITAL = UUID.fromString("01930000-0000-7000-8000-000000000004");

    private static final UUID STRANGER = UUID.fromString("01930000-0000-7000-8000-000000000005");

    private static final UUID ACTOR = UUID.fromString("01930000-0000-7000-8000-000000000006");

    private static final Instant DISPATCHED = Instant.parse("2026-04-01T08:00:00Z");

    private static final Instant FIRST_HANDOFF = DISPATCHED.plus(40, ChronoUnit.MINUTES);

    private static final Instant SECOND_HANDOFF = DISPATCHED.plus(200, ChronoUnit.MINUTES);

    private static final List<ShipmentParticipant> EVERYONE = List.of(
            participant(LABORATORY, Participation.SHIPPER),
            participant(CARRIER, Participation.CARRIER),
            participant(HOSPITAL, Participation.CONSIGNEE));

    @Test
    void theHolderAtAMomentIsWhoeverTheLatestEventBeforeItLeftHoldingTheShipment() {
        CustodyHistory history = CustodyHistory.restore(journey(), EVERYONE);

        assertThat(history.holderAt(DISPATCHED.plus(10, ChronoUnit.MINUTES)))
                .isEqualTo(Participation.SHIPPER);
        assertThat(history.holderAt(FIRST_HANDOFF.plus(1, ChronoUnit.MINUTES)))
                .isEqualTo(Participation.CARRIER);
        assertThat(history.holderAt(FIRST_HANDOFF))
                .describedAs("a handoff at the very moment the excursion opens already moved custody")
                .isEqualTo(Participation.CARRIER);
        assertThat(history.holderAt(SECOND_HANDOFF.plus(1, ChronoUnit.MINUTES)))
                .isEqualTo(Participation.CONSIGNEE);
    }

    @Test
    void theChainIsReadInTheOrderItWasWrittenWhateverOrderItArrivesIn() {
        CustodyHistory history = CustodyHistory.restore(journey().reversed(), EVERYONE);

        assertThat(history.holderAt(FIRST_HANDOFF.plus(1, ChronoUnit.MINUTES)))
                .isEqualTo(Participation.CARRIER);
    }

    @Test
    void theTimeSinceTheLastHandoffIsMeasuredFromTheOneBeforeTheMoment() {
        CustodyHistory history = CustodyHistory.restore(journey(), EVERYONE);

        assertThat(history.sinceLastHandoffAt(FIRST_HANDOFF.plus(25, ChronoUnit.MINUTES)))
                .contains(Duration.ofMinutes(25));
        assertThat(history.sinceLastHandoffAt(DISPATCHED.plus(10, ChronoUnit.MINUTES)))
                .describedAs("a shipment still in its shipper's hands has never changed hands")
                .isEmpty();
    }

    @Test
    void theHandoffsOfAWindowAreNamedByParticipationAndBothEdgesCount() {
        CustodyHistory history = CustodyHistory.restore(journey(), EVERYONE);

        assertThat(history.handoffsBetween(FIRST_HANDOFF, SECOND_HANDOFF)).containsExactly(
                new CustodyHandoff(FIRST_HANDOFF, Participation.SHIPPER, Participation.CARRIER),
                new CustodyHandoff(SECOND_HANDOFF, Participation.CARRIER, Participation.CONSIGNEE));
        assertThat(history.handoffsBetween(DISPATCHED, FIRST_HANDOFF.minusSeconds(1))).isEmpty();
    }

    @Test
    void aCarrierRevokedLaterStillHeldCustodyWhenItHeldIt() {
        CustodyHistory history = CustodyHistory.restore(journey(), List.of(
                participant(LABORATORY, Participation.SHIPPER),
                participant(CARRIER, Participation.CARRIER).revoke(SECOND_HANDOFF.plusSeconds(60)),
                participant(HOSPITAL, Participation.CONSIGNEE)));

        assertThat(history.holderAt(FIRST_HANDOFF.plus(1, ChronoUnit.MINUTES)))
                .describedAs("leaving the shipment afterwards does not rewrite who held it")
                .isEqualTo(Participation.CARRIER);
    }

    @Test
    void theLiveParticipationWinsOverOneThatWasRevoked() {
        CustodyHistory history = CustodyHistory.restore(journey(), List.of(
                participant(LABORATORY, Participation.SHIPPER),
                participant(CARRIER, Participation.OBSERVER).revoke(DISPATCHED.plusSeconds(60)),
                participant(CARRIER, Participation.CARRIER),
                participant(HOSPITAL, Participation.CONSIGNEE)));

        assertThat(history.holderAt(FIRST_HANDOFF.plus(1, ChronoUnit.MINUTES)))
                .isEqualTo(Participation.CARRIER);
    }

    @Test
    void custodyInTheHandsOfAnOrganizationThatNeverTookPartIsACorruptLog() {
        CustodyHistory history = CustodyHistory.restore(List.of(
                event(1, CustodyEventKind.CREATED, null, LABORATORY, DISPATCHED),
                event(2, CustodyEventKind.HANDOFF, LABORATORY, STRANGER, FIRST_HANDOFF)), EVERYONE);

        assertThatThrownBy(() -> history.holderAt(FIRST_HANDOFF.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aShipmentWithNoCustodyLogHasNoHistory() {
        assertThatThrownBy(() -> CustodyHistory.restore(List.of(), EVERYONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<CustodyEvent> journey() {
        return List.of(
                event(1, CustodyEventKind.CREATED, null, LABORATORY, DISPATCHED.minus(1, ChronoUnit.DAYS)),
                event(2, CustodyEventKind.DISPATCHED, null, LABORATORY, DISPATCHED),
                event(3, CustodyEventKind.HANDOFF, LABORATORY, CARRIER, FIRST_HANDOFF),
                event(4, CustodyEventKind.HANDOFF, CARRIER, HOSPITAL, SECOND_HANDOFF),
                event(5, CustodyEventKind.DELIVERED, null, HOSPITAL, SECOND_HANDOFF.plus(1, ChronoUnit.HOURS)));
    }

    private static CustodyEvent event(int sequence, CustodyEventKind kind, UUID from, UUID to,
            Instant occurredAt) {
        return CustodyEvent.restore(UUID.randomUUID(), SHIPMENT, sequence, kind, from, to, null, ACTOR,
                occurredAt, occurredAt, "0".repeat(64), "f".repeat(64));
    }

    private static ShipmentParticipant participant(UUID organization, Participation participation) {
        return ShipmentParticipant.createNew(SHIPMENT, organization, participation);
    }
}
