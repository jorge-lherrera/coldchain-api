package com.coldchain.modules.telemetry.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResult(UUID id, UUID deviceId, UUID shipmentId, Instant attachedAt,
        Instant detachedAt) {
}
