package com.coldchain.modules.telemetry.api.event;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import java.time.Instant;
import java.util.UUID;

public record ExcursionClosed(UUID excursionId, UUID organizationId, UUID shipmentId,
        ExcursionKind kind, long durationMinutes, Instant closedAt) {
}
