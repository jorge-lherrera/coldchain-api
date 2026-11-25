package com.coldchain.triage.api.dto;

import com.coldchain.triage.api.ExcursionEventType;
import com.coldchain.triage.api.ExcursionKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecordExcursionEventCommand(UUID eventId, ExcursionEventType type, Instant occurredAt,
        UUID shipmentId, UUID organizationId, UUID excursionId, ExcursionKind kind, BigDecimal peakCelsius,
        Long durationMinutes) {
}
