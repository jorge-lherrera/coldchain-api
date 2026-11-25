package com.coldchain.triage.internal.domain.model;

import com.coldchain.triage.api.ExcursionKind;
import com.coldchain.triage.shared.util.UuidV7;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ExcursionAlert {

    private final UUID id;

    private final UUID excursionId;

    private final UUID shipmentId;

    private final UUID organizationId;

    private final ExcursionKind kind;

    private final AlertStatus status;

    private final Instant openedAt;

    private final Instant closedAt;

    private final BigDecimal peakCelsius;

    private final Long durationMinutes;

    private final Triage triage;

    private final Integer version;

    private ExcursionAlert(UUID id, UUID excursionId, UUID shipmentId, UUID organizationId, ExcursionKind kind,
            AlertStatus status, Instant openedAt, Instant closedAt, BigDecimal peakCelsius,
            Long durationMinutes, Triage triage, Integer version) {
        this.id = Objects.requireNonNull(id);
        this.excursionId = Objects.requireNonNull(excursionId);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.kind = Objects.requireNonNull(kind);
        this.status = Objects.requireNonNull(status);
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.peakCelsius = peakCelsius;
        this.durationMinutes = durationMinutes;
        this.triage = triage;
        this.version = version;
    }

    public static ExcursionAlert firstHeardFrom(ExcursionEvent event) {
        ExcursionAlert unheard = new ExcursionAlert(UuidV7.generate(), event.excursionId(), event.shipmentId(),
                event.organizationId(), event.kind(), AlertStatus.OPEN, null, null, null, null, null, null);
        return unheard.apply(event);
    }

    public static ExcursionAlert restore(UUID id, UUID excursionId, UUID shipmentId, UUID organizationId,
            ExcursionKind kind, AlertStatus status, Instant openedAt, Instant closedAt, BigDecimal peakCelsius,
            Long durationMinutes, Triage triage, Integer version) {
        return new ExcursionAlert(id, excursionId, shipmentId, organizationId, kind, status, openedAt, closedAt,
                peakCelsius, durationMinutes, triage, version);
    }

    public ExcursionAlert apply(ExcursionEvent event) {
        if (!excursionId.equals(event.excursionId())) {
            throw new IllegalArgumentException(
                    "Event " + event.eventId() + " belongs to excursion " + event.excursionId() + ", not " + excursionId);
        }
        return switch (event.type()) {
            case OPENED -> new ExcursionAlert(id, excursionId, shipmentId, organizationId, kind, status,
                    event.occurredAt(), closedAt, event.peakCelsius(), durationMinutes, triage, version);
            case CLOSED -> new ExcursionAlert(id, excursionId, shipmentId, organizationId, kind,
                    AlertStatus.CLOSED, openedAt, event.occurredAt(), peakCelsius, event.durationMinutes(), triage,
                    version);
        };
    }

    public ExcursionAlert triagedBy(Triage candidate) {
        if (triage != null && triage.byModel() && !candidate.byModel()) {
            return this;
        }
        return new ExcursionAlert(id, excursionId, shipmentId, organizationId, kind, status, openedAt, closedAt,
                peakCelsius, durationMinutes, candidate, version);
    }

    public boolean closed() {
        return status == AlertStatus.CLOSED;
    }

    public UUID id() {
        return id;
    }

    public UUID excursionId() {
        return excursionId;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public ExcursionKind kind() {
        return kind;
    }

    public AlertStatus status() {
        return status;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public BigDecimal peakCelsius() {
        return peakCelsius;
    }

    public Long durationMinutes() {
        return durationMinutes;
    }

    public Triage triage() {
        return triage;
    }

    public Integer version() {
        return version;
    }
}
