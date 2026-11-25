package com.coldchain.triage.internal.domain.model;

import com.coldchain.triage.api.ExcursionEventType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InboxEvent(UUID eventId, ExcursionEventType type, UUID excursionId, Instant occurredAt,
        Instant receivedAt, InboxStatus status, Instant processedAt) {

    public InboxEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(excursionId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(receivedAt);
        Objects.requireNonNull(status);
        if ((status == InboxStatus.PROCESSED) != (processedAt != null)) {
            throw new IllegalArgumentException("A processed event says when it was processed, and only then");
        }
    }

    public static InboxEvent received(ExcursionEvent event, Instant receivedAt) {
        return new InboxEvent(event.eventId(), event.type(), event.excursionId(), event.occurredAt(), receivedAt,
                InboxStatus.RECEIVED, null);
    }

    public InboxEvent processedAt(Instant when) {
        return new InboxEvent(eventId, type, excursionId, occurredAt, receivedAt, InboxStatus.PROCESSED, when);
    }

    public boolean processed() {
        return status == InboxStatus.PROCESSED;
    }
}
