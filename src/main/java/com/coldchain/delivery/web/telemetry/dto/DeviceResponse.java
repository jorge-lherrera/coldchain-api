package com.coldchain.delivery.web.telemetry.dto;

import com.coldchain.modules.telemetry.api.DeviceStatus;
import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(UUID id, String serialNumber, String model, String firmware,
        DeviceStatus status, int samplingIntervalSeconds, Instant calibratedAt) {
}
