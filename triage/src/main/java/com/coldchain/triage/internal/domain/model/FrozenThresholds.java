package com.coldchain.triage.internal.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record FrozenThresholds(BigDecimal minCelsius, BigDecimal maxCelsius, int maxSingleExcursionMinutes,
        int maxCumulativeExcursionMinutes) {

    public FrozenThresholds {
        Objects.requireNonNull(minCelsius);
        Objects.requireNonNull(maxCelsius);
        if (maxSingleExcursionMinutes <= 0) {
            throw new IllegalArgumentException("A single excursion is allowed some minutes, received "
                    + maxSingleExcursionMinutes);
        }
    }
}
