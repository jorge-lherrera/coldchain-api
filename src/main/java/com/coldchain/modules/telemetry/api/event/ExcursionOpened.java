package com.coldchain.modules.telemetry.api.event;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExcursionOpened(UUID excursionId, UUID organizationId, UUID shipmentId,
        ExcursionKind kind, BigDecimal peakCelsius, Instant openedAt) implements TelemetryEvent {

    @Override
    public Instant occurredAt() {
        return openedAt;
    }
}
