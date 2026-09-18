package com.coldchain.modules.telemetry.internal.application;

import com.coldchain.modules.telemetry.api.event.ExcursionClosed;
import com.coldchain.modules.telemetry.api.event.ExcursionOpened;
import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import com.coldchain.modules.telemetry.internal.domain.model.Excursion;
import com.coldchain.modules.telemetry.internal.domain.model.TemperatureReading;
import com.coldchain.modules.telemetry.internal.domain.repository.ExcursionRepository;
import com.coldchain.modules.telemetry.internal.domain.repository.TemperatureReadingRepository;
import com.coldchain.modules.telemetry.internal.domain.service.DetectedExcursion;
import com.coldchain.modules.telemetry.internal.domain.service.ExcursionDetector;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExcursionReview {

    private final TemperatureReadingRepository readings;

    private final ExcursionRepository excursions;

    private final ApplicationEventPublisher events;

    public ExcursionReview(TemperatureReadingRepository readings, ExcursionRepository excursions,
            ApplicationEventPublisher events) {
        this.readings = readings;
        this.excursions = excursions;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reviewShipmentsTouchedBy(List<TemperatureReading> accepted,
            List<DeviceAssignment> windows) {
        Set<UUID> touched = accepted.stream()
                .map(TemperatureReading::shipmentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        touched.forEach(shipmentId -> review(shipmentId, thresholdsOf(windows, shipmentId)));
    }

    private void review(UUID shipmentId, DeviceAssignment window) {
        if (window == null) {
            return;
        }
        List<TemperatureReading> series = readings.findOfShipment(shipmentId);
        Map<Instant, Excursion> before = excursions.findOfShipment(shipmentId).stream()
                .collect(Collectors.toMap(Excursion::openedAt, Function.identity(),
                        (first, second) -> first));
        excursions.deleteOfShipment(shipmentId);

        UUID organizationId = series.isEmpty() ? null : series.getFirst().organizationId();
        for (DetectedExcursion detected : ExcursionDetector.detect(series, window.minCelsius(),
                window.maxCelsius())) {
            Excursion opened = Excursion.open(organizationId, shipmentId, detected.kind(),
                    detected.openedAt(), detected.openedByReadingId(), detected.peakCelsius())
                    .deepenedTo(detected.peakCelsius(), detected.lastSeenAt());
            Excursion stored = detected.closed()
                    ? opened.close(detected.closedAt(), detected.closedByReadingId())
                    : opened;
            excursions.save(stored);
            Excursion previous = before.get(detected.openedAt());
            if (previous == null) {
                events.publishEvent(new ExcursionOpened(stored.id(), organizationId, shipmentId,
                        stored.kind(), stored.peakCelsius(), stored.openedAt()));
            }
            if (stored.closedAt() != null && (previous == null || previous.open())) {
                events.publishEvent(new ExcursionClosed(stored.id(), organizationId, shipmentId,
                        stored.kind(), stored.durationMinutes(), stored.closedAt()));
            }
        }
    }

    private static DeviceAssignment thresholdsOf(List<DeviceAssignment> windows, UUID shipmentId) {
        return windows.stream()
                .filter(window -> window.shipmentId().equals(shipmentId))
                .findFirst()
                .orElse(null);
    }
}
