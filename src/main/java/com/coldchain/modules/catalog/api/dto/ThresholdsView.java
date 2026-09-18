package com.coldchain.modules.catalog.api.dto;

import java.math.BigDecimal;

public record ThresholdsView(
        BigDecimal minCelsius,
        BigDecimal maxCelsius,
        int maxSingleExcursionMinutes,
        int maxCumulativeExcursionMinutes,
        BigDecimal minCoveragePercent) {
}
