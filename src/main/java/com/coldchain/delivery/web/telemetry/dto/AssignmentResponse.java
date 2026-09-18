package com.coldchain.delivery.web.telemetry.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(UUID id, UUID deviceId, UUID shipmentId, Instant attachedAt,
        Instant detachedAt) {
}
