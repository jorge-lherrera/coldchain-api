package com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
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
@Table(name = "DEVICE_ASSIGNMENT", indexes = {
        @Index(name = "IX_DEVICE_ASSIGNMENT_DEVICE_ID", columnList = "DEVICE_ID"), 
        @Index(name = "IX_DEVICE_ASSIGNMENT_SHIPMENT_ID", columnList = "SHIPMENT_ID")})
public class DeviceAssignmentJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "DEVICE_ID", nullable = false)
    private UUID deviceId;

    @Column(name = "SHIPMENT_ID", nullable = false)
    private UUID shipmentId;

    @Column(name = "MIN_CELSIUS")
    private BigDecimal minCelsius;

    @Column(name = "MAX_CELSIUS")
    private BigDecimal maxCelsius;

    @Column(name = "ATTACHED_AT", nullable = false)
    private Instant attachedAt;

    @Column(name = "DETACHED_AT")
    private Instant detachedAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected DeviceAssignmentJpaEntity() {
    }

    public DeviceAssignmentJpaEntity(UUID id, UUID deviceId, UUID shipmentId, BigDecimal minCelsius, BigDecimal maxCelsius, Instant attachedAt, Instant detachedAt, long lockVersion) {
        this.id = id;
        this.deviceId = deviceId;
        this.shipmentId = shipmentId;
        this.minCelsius = minCelsius;
        this.maxCelsius = maxCelsius;
        this.attachedAt = attachedAt;
        this.detachedAt = detachedAt;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public BigDecimal getMinCelsius() {
        return minCelsius;
    }

    public BigDecimal getMaxCelsius() {
        return maxCelsius;
    }

    public Instant getAttachedAt() {
        return attachedAt;
    }

    public Instant getDetachedAt() {
        return detachedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
