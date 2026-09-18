package com.coldchain.modules.telemetry.api.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterDeviceCommand(UUID organizationId, String serialNumber, String model,
        String firmware, int samplingIntervalSeconds, Instant calibratedAt) {
}
