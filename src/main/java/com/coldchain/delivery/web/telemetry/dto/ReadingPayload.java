package com.coldchain.delivery.web.telemetry.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record ReadingPayload(@NotNull Instant measuredAt, @NotNull BigDecimal celsius) {
}
