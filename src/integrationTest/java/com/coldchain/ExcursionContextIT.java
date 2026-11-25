package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.modules.shipment.api.dto.AcceptHandoffCommand;
import com.coldchain.modules.shipment.api.dto.CustodyChange;
import com.coldchain.modules.shipment.api.dto.CustodyEventResult;
import com.coldchain.modules.shipment.api.dto.ExcursionContextResult;
import com.coldchain.modules.shipment.api.dto.OpenHandoffCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.api.ExcursionStatus;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.ExcursionResult;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.ReadingCommand;
import com.coldchain.modules.telemetry.api.dto.SeriesPoint;
import com.coldchain.shared.exception.DomainException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ExcursionContextIT {

    private static final int JOURNEY_MINUTES = 180;

    private static final int SAMPLE_MINUTES = 5;

    private static final int BLACKOUT_FROM = 110;

    private static final int BLACKOUT_TO = 165;

    private static final BigDecimal IN_BAND = new BigDecimal("4.50");

    private static final BigDecimal TOO_WARM = new BigDecimal("11.50");

    @Autowired
    private Fixtures fixtures;

    @Autowired
    private ShipmentApi shipments;

    @Autowired
    private TelemetryApi telemetry;

    @Test
    void theWalkthroughsExcursionComesWithItsReadings_itsThresholdsAndTheCarrierHoldingIt() {
        Journey journey = handedToACarrier();
        ExcursionResult excursion = excursionOf(journey, 60, 100);

        ExcursionContextResult context = shipments.excursionContextOf(journey.shipment().id(),
                excursion.id());

        Instant dispatched = journey.shipment().dispatchedAt();
        assertThat(context.excursion().id()).isEqualTo(excursion.id());
        assertThat(context.excursion().kind()).isEqualTo(ExcursionKind.ABOVE_MAX);
        assertThat(context.excursion().status()).isEqualTo(ExcursionStatus.CLOSED);
        assertThat(context.excursion().peakCelsius()).isEqualByComparingTo(TOO_WARM);
        assertThat(context.windowFrom())
                .describedAs("the window opens thirty minutes before the excursion did")
                .isEqualTo(dispatched.plus(30, ChronoUnit.MINUTES));
        assertThat(context.windowTo())
                .describedAs("and closes thirty minutes after the first reading back in band")
                .isEqualTo(dispatched.plus(135, ChronoUnit.MINUTES));
        assertThat(context.readings())
                .describedAs("every sample from minute 30 to minute 105; the blackout leaves the rest "
                        + "of the window empty")
                .hasSize(16)
                .extracting(SeriesPoint::measuredAt)
                .allSatisfy(measured -> assertThat(measured)
                        .isBetween(context.windowFrom(), context.windowTo()))
                .isSorted();
        assertThat(context.thresholds().minCelsius()).isEqualByComparingTo("2.00");
        assertThat(context.thresholds().maxCelsius()).isEqualByComparingTo("8.00");
        assertThat(context.thresholds().maxSingleExcursionMinutes()).isEqualTo(30);
        assertThat(context.custodian())
                .describedAs("the carrier accepted the handoff before the temperature rose")
                .isEqualTo(Participation.CARRIER);
        assertThat(context.minutesSinceLastHandoff())
                .isEqualTo(Duration.between(journey.handoffAt(), excursion.openedAt()).toMinutes());
        assertThat(context.handoffs())
                .describedAs("the only handoff happened an hour before, outside the window")
                .isEmpty();
    }

    @Test
    void aHandoffInsideTheWindowIsNamedByWhoGaveAndWhoTook() {
        Journey journey = handedToACarrier();
        ExcursionResult excursion = excursionOf(journey, 10, 20);

        ExcursionContextResult context = shipments.excursionContextOf(journey.shipment().id(),
                excursion.id());

        assertThat(context.handoffs()).containsExactly(
                new CustodyChange(journey.handoffAt(), Participation.SHIPPER, Participation.CARRIER));
        assertThat(context.custodian()).isEqualTo(Participation.CARRIER);
    }

    @Test
    void anExcursionIsOnlyFoundUnderItsOwnShipment() {
        Journey first = handedToACarrier();
        Journey second = handedToACarrier();
        ExcursionResult elsewhere = excursionOf(second, 60, 70);

        assertThatThrownBy(() -> shipments.excursionContextOf(first.shipment().id(), elsewhere.id()))
                .describedAs("a real excursion under the wrong shipment is no excursion at all")
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("EXCURSION_NOT_FOUND");
        assertThatThrownBy(() -> shipments.excursionContextOf(UUID.randomUUID(), elsewhere.id()))
                .isInstanceOf(DomainException.class)
                .extracting(failure -> ((DomainException) failure).errorCode().name())
                .isEqualTo("SHIPMENT_NOT_FOUND");
    }

    private Journey handedToACarrier() {
        RegisterOrganizationResult laboratory = fixtures.organization("laboratory");
        RegisterOrganizationResult carrier = fixtures.organization("carrier");
        DeviceResult device = fixtures.device(laboratory);
        ShipmentResult dispatched = fixtures.dispatchedShipment(laboratory, device);
        OpenHandoffResult handoff = ActingAs.user(laboratory.administratorId(),
                laboratory.organizationId(), () -> shipments.openHandoff(
                        new OpenHandoffCommand(dispatched.id(), carrier.organizationId())));
        ActingAs.user(carrier.administratorId(), carrier.organizationId(),
                () -> shipments.acceptHandoff(new AcceptHandoffCommand(dispatched.id(),
                        carrier.organizationId(), handoff.code())));
        Instant handoffAt = ActingAs.user(laboratory.administratorId(), laboratory.organizationId(),
                () -> shipments.timelineOf(dispatched.id(), laboratory.organizationId())).events().stream()
                .filter(event -> event.kind() == CustodyEventKind.HANDOFF)
                .map(CustodyEventResult::occurredAt)
                .findFirst()
                .orElseThrow();
        return new Journey(laboratory, device, dispatched, handoffAt);
    }

    private ExcursionResult excursionOf(Journey journey, int warmFrom, int warmTo) {
        List<ReadingCommand> readings = new ArrayList<>();
        for (int minute = 0; minute <= JOURNEY_MINUTES; minute += SAMPLE_MINUTES) {
            if (minute >= BLACKOUT_FROM && minute <= BLACKOUT_TO) {
                continue;
            }
            readings.add(new ReadingCommand(
                    journey.shipment().dispatchedAt().plus(minute, ChronoUnit.MINUTES),
                    minute >= warmFrom && minute <= warmTo ? TOO_WARM : IN_BAND));
        }
        telemetry.ingestBatch(new IngestBatchCommand(journey.owner().organizationId(),
                journey.device().id(), "context-" + UUID.randomUUID(), readings));
        return telemetry.seriesOf(journey.shipment().id(), journey.owner().organizationId())
                .excursions().getFirst();
    }

    private record Journey(RegisterOrganizationResult owner, DeviceResult device,
            ShipmentResult shipment, Instant handoffAt) {
    }
}
