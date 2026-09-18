package com.coldchain.modules.telemetry.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignDeviceCommand(UUID deviceId, UUID shipmentId, MonitoringThresholds thresholds,
        Instant attachedAt) {
}
