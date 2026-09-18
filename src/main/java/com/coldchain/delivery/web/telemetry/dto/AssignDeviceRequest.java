package com.coldchain.delivery.web.telemetry.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssignDeviceRequest(
        @NotNull UUID shipmentId,
        @NotNull BigDecimal minCelsius,
        @NotNull BigDecimal maxCelsius,
        @NotNull Instant attachedAt) {
}
