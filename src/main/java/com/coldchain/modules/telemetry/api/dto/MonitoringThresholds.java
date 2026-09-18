package com.coldchain.modules.telemetry.api.dto;

import java.math.BigDecimal;

public record MonitoringThresholds(BigDecimal minCelsius, BigDecimal maxCelsius) {
}
