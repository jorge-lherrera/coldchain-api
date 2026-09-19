package com.coldchain.modules.telemetry.internal.domain.model;

import com.coldchain.modules.telemetry.api.DeviceStatus;
import com.coldchain.shared.util.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Device {

    private final UUID id;

    private final UUID organizationId;

    private final String serialNumber;

    private final String model;

    private final String firmware;

    private final DeviceStatus status;

    private final int samplingIntervalSeconds;

    private final Instant calibratedAt;

    private final Instant deletedAt;

    private Device(UUID id, UUID organizationId, String serialNumber, String model,
            String firmware, DeviceStatus status, int samplingIntervalSeconds, Instant calibratedAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.serialNumber = Objects.requireNonNull(serialNumber);
        this.model = Objects.requireNonNull(model);
        this.firmware = Objects.requireNonNull(firmware);
        this.status = Objects.requireNonNull(status);
        this.samplingIntervalSeconds = samplingIntervalSeconds;
        this.calibratedAt = Objects.requireNonNull(calibratedAt);
        this.deletedAt = deletedAt;
        if (samplingIntervalSeconds <= 0) {
            throw new IllegalArgumentException("A device that never samples measures nothing");
        }
    }

    public static Device register(UUID organizationId, String serialNumber, String model,
            String firmware, int samplingIntervalSeconds, Instant calibratedAt) {
        return new Device(UuidV7.generate(), organizationId, serialNumber, model, firmware,
                DeviceStatus.ACTIVE, samplingIntervalSeconds, calibratedAt, null);
    }

    public static Device restore(UUID id, UUID organizationId, String serialNumber, String model,
            String firmware, DeviceStatus status, int samplingIntervalSeconds, Instant calibratedAt,
            Instant deletedAt) {
        return new Device(id, organizationId, serialNumber, model, firmware, status,
                samplingIntervalSeconds, calibratedAt, deletedAt);
    }

    public boolean usable() {
        return status == DeviceStatus.ACTIVE && deletedAt == null;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String serialNumber() {
        return serialNumber;
    }

    public String model() {
        return model;
    }

    public String firmware() {
        return firmware;
    }

    public DeviceStatus status() {
        return status;
    }

    public int samplingIntervalSeconds() {
        return samplingIntervalSeconds;
    }

    public Instant calibratedAt() {
        return calibratedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }
}
