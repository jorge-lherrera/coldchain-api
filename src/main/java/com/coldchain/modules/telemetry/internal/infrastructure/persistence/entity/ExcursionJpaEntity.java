package com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity;

import com.coldchain.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "EXCURSION", indexes = {
        @Index(name = "IX_EXCURSION_ORGANIZATION_ID", columnList = "ORGANIZATION_ID")})
public class ExcursionJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "SHIPMENT_ID", nullable = false)
    private UUID shipmentId;

    @Column(name = "KIND", nullable = false)
    private String kind;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "OPENED_AT", nullable = false)
    private Instant openedAt;

    @Column(name = "CLOSED_AT")
    private Instant closedAt;

    @Column(name = "OPENED_BY_READING_ID", nullable = false)
    private UUID openedByReadingId;

    @Column(name = "CLOSED_BY_READING_ID")
    private UUID closedByReadingId;

    @Column(name = "PEAK_CELSIUS", nullable = false)
    private BigDecimal peakCelsius;

    @Column(name = "DURATION_MINUTES", nullable = false)
    private long durationMinutes;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected ExcursionJpaEntity() {
    }

    public ExcursionJpaEntity(UUID id, UUID organizationId, UUID shipmentId, String kind, String status, Instant openedAt, Instant closedAt, UUID openedByReadingId, UUID closedByReadingId, BigDecimal peakCelsius, long durationMinutes, long lockVersion) {
        this.id = id;
        this.organizationId = organizationId;
        this.shipmentId = shipmentId;
        this.kind = kind;
        this.status = status;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.openedByReadingId = openedByReadingId;
        this.closedByReadingId = closedByReadingId;
        this.peakCelsius = peakCelsius;
        this.durationMinutes = durationMinutes;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public String getKind() {
        return kind;
    }

    public String getStatus() {
        return status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public UUID getOpenedByReadingId() {
        return openedByReadingId;
    }

    public UUID getClosedByReadingId() {
        return closedByReadingId;
    }

    public BigDecimal getPeakCelsius() {
        return peakCelsius;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
