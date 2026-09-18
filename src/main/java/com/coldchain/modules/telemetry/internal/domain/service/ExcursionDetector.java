package com.coldchain.modules.telemetry.internal.domain.service;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.internal.domain.model.TemperatureReading;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ExcursionDetector {

    private ExcursionDetector() {
    }

    public static List<DetectedExcursion> detect(List<TemperatureReading> series,
            BigDecimal minCelsius, BigDecimal maxCelsius) {
        List<TemperatureReading> ordered = series.stream()
                .sorted(Comparator.comparing(TemperatureReading::measuredAt))
                .toList();
        List<DetectedExcursion> found = new ArrayList<>();
        DetectedExcursion current = null;
        for (TemperatureReading reading : ordered) {
            Optional<ExcursionKind> breach = breachOf(reading.celsius(), minCelsius, maxCelsius);
            if (breach.isPresent()) {
                if (current == null || current.kind() != breach.get()) {
                    if (current != null) {
                        found.add(current.closedBy(reading));
                        current = null;
                    }
                    current = DetectedExcursion.openedBy(breach.get(), reading);
                } else {
                    current = current.deepenedBy(reading);
                }
            } else if (current != null) {
                found.add(current.closedBy(reading));
                current = null;
            }
        }
        if (current != null) {
            found.add(current);
        }
        return List.copyOf(found);
    }

    private static Optional<ExcursionKind> breachOf(BigDecimal celsius, BigDecimal minCelsius,
            BigDecimal maxCelsius) {
        if (celsius.compareTo(maxCelsius) > 0) {
            return Optional.of(ExcursionKind.ABOVE_MAX);
        }
        if (celsius.compareTo(minCelsius) < 0) {
            return Optional.of(ExcursionKind.BELOW_MIN);
        }
        return Optional.empty();
    }
}
