package com.coldchain.modules.compliance.internal.domain.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record EvaluationInput(
        BigDecimal minCelsius,
        BigDecimal maxCelsius,
        int maxSingleExcursionMinutes,
        int maxCumulativeExcursionMinutes,
        BigDecimal minCoveragePercent,
        Instant evaluatedFrom,
        Instant evaluatedTo,
        int samplingIntervalSeconds,
        int readingsReceived,
        boolean calibrationValid,
        boolean deviceAssigned,
        List<ExcursionFact> excursions) {
}
