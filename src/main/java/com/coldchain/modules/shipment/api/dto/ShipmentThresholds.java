package com.coldchain.modules.shipment.api.dto;

import java.math.BigDecimal;

public record ShipmentThresholds(
        BigDecimal minCelsius,
        BigDecimal maxCelsius,
        int maxSingleExcursionMinutes,
        int maxCumulativeExcursionMinutes,
        BigDecimal minCoveragePercent) {
}
