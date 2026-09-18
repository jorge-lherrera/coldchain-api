package com.coldchain.modules.shipment.internal.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record FrozenThresholds(
        BigDecimal minCelsius,
        BigDecimal maxCelsius,
        int maxSingleExcursionMinutes,
        int maxCumulativeExcursionMinutes,
        BigDecimal minCoveragePercent) {

    public FrozenThresholds {
        Objects.requireNonNull(minCelsius);
        Objects.requireNonNull(maxCelsius);
        Objects.requireNonNull(minCoveragePercent);
        if (minCelsius.compareTo(maxCelsius) >= 0) {
            throw new IllegalArgumentException("The frozen range is upside down");
        }
    }
}
