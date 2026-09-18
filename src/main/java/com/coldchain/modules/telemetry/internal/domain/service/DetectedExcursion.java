package com.coldchain.modules.telemetry.internal.domain.service;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.internal.domain.model.TemperatureReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DetectedExcursion(
        ExcursionKind kind,
        Instant openedAt,
        UUID openedByReadingId,
        Instant lastSeenAt,
        Instant closedAt,
        UUID closedByReadingId,
        BigDecimal peakCelsius) {

    static DetectedExcursion openedBy(ExcursionKind kind, TemperatureReading reading) {
        return new DetectedExcursion(kind, reading.measuredAt(), reading.id(), reading.measuredAt(),
                null, null, reading.celsius());
    }

    DetectedExcursion deepenedBy(TemperatureReading reading) {
        BigDecimal peak = kind == ExcursionKind.ABOVE_MAX
                ? peakCelsius.max(reading.celsius())
                : peakCelsius.min(reading.celsius());
        return new DetectedExcursion(kind, openedAt, openedByReadingId, reading.measuredAt(), null,
                null, peak);
    }

    DetectedExcursion closedBy(TemperatureReading reading) {
        return new DetectedExcursion(kind, openedAt, openedByReadingId, lastSeenAt,
                reading.measuredAt(), reading.id(), peakCelsius);
    }

    public boolean closed() {
        return closedAt != null;
    }
}
