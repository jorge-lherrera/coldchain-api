package com.coldchain.modules.catalog.internal.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Thresholds(
        BigDecimal minCelsius,
        BigDecimal maxCelsius,
        int maxSingleExcursionMinutes,
        int maxCumulativeExcursionMinutes,
        BigDecimal minCoveragePercent) {

    public Thresholds {
        Objects.requireNonNull(minCelsius);
        Objects.requireNonNull(maxCelsius);
        Objects.requireNonNull(minCoveragePercent);
        if (minCelsius.compareTo(maxCelsius) >= 0) {
            throw new IllegalArgumentException("The minimum temperature is below the maximum one");
        }
        if (maxSingleExcursionMinutes < 0 || maxCumulativeExcursionMinutes < 0) {
            throw new IllegalArgumentException("An excursion allowance is never negative");
        }
        if (maxSingleExcursionMinutes > maxCumulativeExcursionMinutes) {
            throw new IllegalArgumentException(
                    "A single excursion cannot be longer than everything allowed together");
        }
        if (minCoveragePercent.compareTo(BigDecimal.ZERO) < 0
                || minCoveragePercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Coverage is a percentage between 0 and 100");
        }
    }
}
