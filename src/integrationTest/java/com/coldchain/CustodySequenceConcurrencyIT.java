package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.modules.shipment.api.dto.CustodyEventResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.api.dto.ShipmentTimelineResult;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CustodySequenceConcurrencyIT {

    @Autowired
    private ShipmentApi shipments;

    @Autowired
    private Fixtures fixtures;

    @Test
    void noTwoEventsShareASequenceNumber() throws Exception {
        RegisterOrganizationResult owner = fixtures.organization("custody");
        DeviceResult device = fixtures.device(owner);
        ShipmentResult shipment = fixtures.dispatchedShipment(owner, device);

        List<Boolean> outcomes = raceToArrive(owner, shipment);
        ShipmentTimelineResult timeline = ActingAs.user(owner.administratorId(),
                owner.organizationId(),
                () -> shipments.timelineOf(shipment.id(), owner.organizationId()));
        List<Integer> sequence = timeline.events().stream()
                .map(CustodyEventResult::sequenceNumber)
                .toList();

        assertThat(outcomes)
                .describedAs("two callers raced for the same next number and the loser was told "
                        + "why, instead of colliding on a unique index")
                .containsExactlyInAnyOrder(true, false);
        assertThat(sequence)
                .describedAs("a chain with a repeated link is not a chain")
                .doesNotHaveDuplicates()
                .isSorted()
                .containsExactlyElementsOf(oneUpTo(sequence.size()));
        assertThat(timeline.verdict().intact()).isTrue();
    }

    private List<Boolean> raceToArrive(RegisterOrganizationResult owner, ShipmentResult shipment) {
        Callable<Boolean> arrive = () -> ActingAs.user(owner.administratorId(),
                owner.organizationId(), () -> {
                    try {
                        shipments.arriveShipment(shipment.id());
                        return true;
                    } catch (RuntimeException refused) {
                        return false;
                    }
                });
        try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> races = threads.invokeAll(List.of(arrive, arrive), 30,
                    TimeUnit.SECONDS);
            return races.stream().map(CustodySequenceConcurrencyIT::outcomeOf).toList();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The race was interrupted", interrupted);
        }
    }

    private static Boolean outcomeOf(Future<Boolean> race) {
        try {
            return race.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The race was interrupted", interrupted);
        } catch (Exception failure) {
            return false;
        }
    }

    private static List<Integer> oneUpTo(int last) {
        return IntStream.rangeClosed(1, last).boxed().toList();
    }
}
