package com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "READING_BATCH", indexes = {
        @Index(name = "IX_READING_BATCH_DEVICE_ID", columnList = "DEVICE_ID")})
public class ReadingBatchJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "DEVICE_ID", nullable = false)
    private UUID deviceId;

    @Column(name = "IDEMPOTENCY_KEY", nullable = false)
    private String idempotencyKey;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "RECEIVED_COUNT", nullable = false)
    private int receivedCount;

    @Column(name = "ACCEPTED_COUNT", nullable = false)
    private int acceptedCount;

    @Column(name = "DISCARDED_COUNT", nullable = false)
    private int discardedCount;

    @Column(name = "RECEIVED_AT", nullable = false)
    private Instant receivedAt;

    protected ReadingBatchJpaEntity() {
    }

    public ReadingBatchJpaEntity(UUID id, UUID organizationId, UUID deviceId, String idempotencyKey, String status, int receivedCount, int acceptedCount, int discardedCount, Instant receivedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.deviceId = deviceId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.receivedCount = receivedCount;
        this.acceptedCount = acceptedCount;
        this.discardedCount = discardedCount;
        this.receivedAt = receivedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getStatus() {
        return status;
    }

    public int getReceivedCount() {
        return receivedCount;
    }

    public int getAcceptedCount() {
        return acceptedCount;
    }

    public int getDiscardedCount() {
        return discardedCount;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
