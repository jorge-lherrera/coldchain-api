package com.coldchain.delivery.web.shipment.dto;

import java.time.Instant;
import java.util.UUID;

public record OpenHandoffResponse(UUID handoffId, String code, Instant expiresAt) {
}
