package com.coldchain.modules.telemetry.internal.domain.model;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.api.ExcursionStatus;
import com.coldchain.shared.identifier.UuidV7;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Excursion {

    private final UUID id;

    private final UUID organizationId;

    private final UUID shipmentId;

    private final ExcursionKind kind;

    private final ExcursionStatus status;

    private final Instant openedAt;

    private final Instant closedAt;

    private final UUID openedByReadingId;

    private final UUID closedByReadingId;

    private final BigDecimal peakCelsius;

    private final long durationMinutes;

    private final long lockVersion;

    private Excursion(UUID id, UUID organizationId, UUID shipmentId, ExcursionKind kind,
            ExcursionStatus status, Instant openedAt, Instant closedAt, UUID openedByReadingId,
            UUID closedByReadingId, BigDecimal peakCelsius, long durationMinutes, long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.kind = Objects.requireNonNull(kind);
        this.status = Objects.requireNonNull(status);
        this.openedAt = Objects.requireNonNull(openedAt);
        this.closedAt = closedAt;
        this.openedByReadingId = Objects.requireNonNull(openedByReadingId);
        this.closedByReadingId = closedByReadingId;
        this.peakCelsius = Objects.requireNonNull(peakCelsius);
        this.durationMinutes = durationMinutes;
        this.lockVersion = lockVersion;
    }

    public static Excursion open(UUID organizationId, UUID shipmentId, ExcursionKind kind,
            Instant openedAt, UUID openedByReadingId, BigDecimal peakCelsius) {
        return new Excursion(UuidV7.generate(), organizationId, shipmentId, kind,
                ExcursionStatus.OPEN, openedAt, null, openedByReadingId, null, peakCelsius, 0L, 0);
    }

    public static Excursion restore(UUID id, UUID organizationId, UUID shipmentId, ExcursionKind kind,
            ExcursionStatus status, Instant openedAt, Instant closedAt, UUID openedByReadingId,
            UUID closedByReadingId, BigDecimal peakCelsius, long durationMinutes, long lockVersion) {
        return new Excursion(id, organizationId, shipmentId, kind, status, openedAt, closedAt,
                openedByReadingId, closedByReadingId, peakCelsius, durationMinutes, lockVersion);
    }

    public Excursion deepenedTo(BigDecimal celsius, Instant lastSeenAt) {
        BigDecimal peak = kind == ExcursionKind.ABOVE_MAX
                ? peakCelsius.max(celsius)
                : peakCelsius.min(celsius);
        return new Excursion(id, organizationId, shipmentId, kind, status, openedAt, closedAt,
                openedByReadingId, closedByReadingId, peak,
                Duration.between(openedAt, lastSeenAt).toMinutes(), lockVersion);
    }

    public Excursion close(Instant when, UUID closedByReading) {
        return new Excursion(id, organizationId, shipmentId, kind, ExcursionStatus.CLOSED, openedAt,
                when, openedByReadingId, closedByReading, peakCelsius,
                Duration.between(openedAt, when).toMinutes(), lockVersion);
    }

    public boolean open() {
        return status == ExcursionStatus.OPEN;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public ExcursionKind kind() {
        return kind;
    }

    public ExcursionStatus status() {
        return status;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public UUID openedByReadingId() {
        return openedByReadingId;
    }

    public UUID closedByReadingId() {
        return closedByReadingId;
    }

    public BigDecimal peakCelsius() {
        return peakCelsius;
    }

    public long durationMinutes() {
        return durationMinutes;
    }

    public long lockVersion() {
        return lockVersion;
    }
}
