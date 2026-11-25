package com.coldchain.triage.internal.domain.model;

import com.coldchain.triage.api.ExcursionEventType;
import com.coldchain.triage.api.ExcursionKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ExcursionEvent(UUID eventId, ExcursionEventType type, Instant occurredAt, UUID shipmentId,
        UUID organizationId, UUID excursionId, ExcursionKind kind, BigDecimal peakCelsius,
        Long durationMinutes) {

    public ExcursionEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(shipmentId);
        Objects.requireNonNull(organizationId);
        Objects.requireNonNull(excursionId);
        Objects.requireNonNull(kind);
        if (type == ExcursionEventType.OPENED && peakCelsius == null) {
            throw new IllegalArgumentException("An opened excursion states its peak");
        }
        if (type == ExcursionEventType.CLOSED && (durationMinutes == null || durationMinutes < 0)) {
            throw new IllegalArgumentException("A closed excursion states how long it lasted");
        }
    }
}
