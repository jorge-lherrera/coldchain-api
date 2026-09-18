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
@Table(name = "SENSOR_DEVICE", indexes = {
        @Index(name = "IX_SENSOR_DEVICE_ORGANIZATION_ID", columnList = "ORGANIZATION_ID")})
public class SensorDeviceJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "SERIAL_NUMBER", nullable = false)
    private String serialNumber;

    @Column(name = "MODEL", nullable = false)
    private String model;

    @Column(name = "FIRMWARE", nullable = false)
    private String firmware;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "SAMPLING_INTERVAL_SECONDS", nullable = false)
    private int samplingIntervalSeconds;

    @Column(name = "CALIBRATED_AT", nullable = false)
    private Instant calibratedAt;

    @Column(name = "DELETED_AT")
    private Instant deletedAt;

    protected SensorDeviceJpaEntity() {
    }

    public SensorDeviceJpaEntity(UUID id, UUID organizationId, String serialNumber, String model, String firmware, String status, int samplingIntervalSeconds, Instant calibratedAt, Instant deletedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.serialNumber = serialNumber;
        this.model = model;
        this.firmware = firmware;
        this.status = status;
        this.samplingIntervalSeconds = samplingIntervalSeconds;
        this.calibratedAt = calibratedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getModel() {
        return model;
    }

    public String getFirmware() {
        return firmware;
    }

    public String getStatus() {
        return status;
    }

    public int getSamplingIntervalSeconds() {
        return samplingIntervalSeconds;
    }

    public Instant getCalibratedAt() {
        return calibratedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
