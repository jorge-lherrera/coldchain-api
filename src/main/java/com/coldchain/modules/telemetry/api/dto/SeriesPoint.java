package com.coldchain.modules.telemetry.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SeriesPoint(Instant measuredAt, BigDecimal celsius) {
}
