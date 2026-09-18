package com.coldchain.modules.telemetry.api.dto;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.api.ExcursionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExcursionResult(UUID id, UUID shipmentId, ExcursionKind kind, ExcursionStatus status,
        Instant openedAt, Instant closedAt, BigDecimal peakCelsius, long durationMinutes) {
}
