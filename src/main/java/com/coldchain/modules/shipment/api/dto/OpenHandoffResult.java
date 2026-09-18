package com.coldchain.modules.shipment.api.dto;

import java.time.Instant;
import java.util.UUID;

public record OpenHandoffResult(UUID handoffId, String code, Instant expiresAt) {
}
