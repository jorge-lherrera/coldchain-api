package com.coldchain.modules.telemetry.internal.infrastructure.messaging;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.api.event.ExcursionClosed;
import com.coldchain.modules.telemetry.api.event.ExcursionOpened;
import com.coldchain.modules.telemetry.api.event.TelemetryEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public record ExcursionFeedMessage(UUID eventId, String type, Instant occurredAt, UUID shipmentId,
        UUID organizationId, UUID excursionId, ExcursionKind kind, BigDecimal peakCelsius,
        Long durationMinutes) {

    static ExcursionFeedMessage of(TelemetryEvent event) {
        return switch (event) {
            case ExcursionOpened opened -> new ExcursionFeedMessage(eventIdOf(opened), "ExcursionOpened",
                    opened.openedAt(), opened.shipmentId(), opened.organizationId(), opened.excursionId(),
                    opened.kind(), opened.peakCelsius(), null);
            case ExcursionClosed closed -> new ExcursionFeedMessage(eventIdOf(closed), "ExcursionClosed",
                    closed.closedAt(), closed.shipmentId(), closed.organizationId(), closed.excursionId(),
                    closed.kind(), null, closed.durationMinutes());
        };
    }

    private static UUID eventIdOf(TelemetryEvent event) {
        String identity = event.getClass().getSimpleName() + ":" + event.excursionId();
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }
}
