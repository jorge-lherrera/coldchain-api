package com.coldchain.modules.telemetry.internal.domain.model;

import com.coldchain.shared.identifier.UuidV7;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class DeviceAssignment {

    private final UUID id;

    private final UUID deviceId;

    private final UUID shipmentId;

    private final BigDecimal minCelsius;

    private final BigDecimal maxCelsius;

    private final Instant attachedAt;

    private final Instant detachedAt;

    private final long lockVersion;

    private DeviceAssignment(UUID id, UUID deviceId, UUID shipmentId, BigDecimal minCelsius,
            BigDecimal maxCelsius, Instant attachedAt, Instant detachedAt, long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.minCelsius = Objects.requireNonNull(minCelsius);
        this.maxCelsius = Objects.requireNonNull(maxCelsius);
        this.attachedAt = Objects.requireNonNull(attachedAt);
        this.detachedAt = detachedAt;
        this.lockVersion = lockVersion;
    }

    public static DeviceAssignment attach(UUID deviceId, UUID shipmentId, BigDecimal minCelsius,
            BigDecimal maxCelsius, Instant attachedAt) {
        return new DeviceAssignment(UuidV7.generate(), deviceId, shipmentId, minCelsius, maxCelsius,
                attachedAt, null, 0);
    }

    public static DeviceAssignment restore(UUID id, UUID deviceId, UUID shipmentId,
            BigDecimal minCelsius, BigDecimal maxCelsius, Instant attachedAt, Instant detachedAt, long lockVersion) {
        return new DeviceAssignment(id, deviceId, shipmentId, minCelsius, maxCelsius, attachedAt,
                detachedAt, lockVersion);
    }

    public DeviceAssignment detach(Instant when) {
        return new DeviceAssignment(id, deviceId, shipmentId, minCelsius, maxCelsius, attachedAt, when, lockVersion);
    }

    public boolean covers(Instant moment) {
        boolean started = !moment.isBefore(attachedAt);
        boolean notFinished = detachedAt == null || moment.isBefore(detachedAt);
        return started && notFinished;
    }

    public boolean open() {
        return detachedAt == null;
    }

    public UUID id() {
        return id;
    }

    public UUID deviceId() {
        return deviceId;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public BigDecimal minCelsius() {
        return minCelsius;
    }

    public BigDecimal maxCelsius() {
        return maxCelsius;
    }

    public Instant attachedAt() {
        return attachedAt;
    }

    public Instant detachedAt() {
        return detachedAt;
    }

    public long lockVersion() {
        return lockVersion;
    }
}
