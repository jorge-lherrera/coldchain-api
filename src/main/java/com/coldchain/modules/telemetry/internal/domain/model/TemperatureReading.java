package com.coldchain.modules.telemetry.internal.domain.model;

import com.coldchain.shared.identifier.UuidV7;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TemperatureReading {

    private final UUID id;

    private final UUID organizationId;

    private final UUID deviceId;

    private final UUID shipmentId;

    private final UUID batchId;

    private final BigDecimal celsius;

    private final Instant measuredAt;

    private TemperatureReading(UUID id, UUID organizationId, UUID deviceId, UUID shipmentId,
            UUID batchId, BigDecimal celsius, Instant measuredAt) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.batchId = Objects.requireNonNull(batchId);
        this.celsius = Objects.requireNonNull(celsius);
        this.measuredAt = Objects.requireNonNull(measuredAt);
    }

    public static TemperatureReading createNew(UUID organizationId, UUID deviceId, UUID shipmentId,
            UUID batchId, BigDecimal celsius, Instant measuredAt) {
        return new TemperatureReading(UuidV7.generate(), organizationId, deviceId, shipmentId, batchId,
                celsius, measuredAt);
    }

    public static TemperatureReading restore(UUID id, UUID organizationId, UUID deviceId,
            UUID shipmentId, UUID batchId, BigDecimal celsius, Instant measuredAt) {
        return new TemperatureReading(id, organizationId, deviceId, shipmentId, batchId, celsius,
                measuredAt);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID deviceId() {
        return deviceId;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public UUID batchId() {
        return batchId;
    }

    public BigDecimal celsius() {
        return celsius;
    }

    public Instant measuredAt() {
        return measuredAt;
    }
}
