package com.coldchain.modules.telemetry.api.dto;

import com.coldchain.modules.telemetry.api.DeviceStatus;
import java.time.Instant;
import java.util.UUID;

public record DeviceResult(UUID id, UUID organizationId, String serialNumber, String model,
        String firmware, DeviceStatus status, int samplingIntervalSeconds, Instant calibratedAt) {
}
