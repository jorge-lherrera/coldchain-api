package com.coldchain.delivery.web.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ThresholdsPayload(
        @NotNull @DecimalMin("-90.00") @DecimalMax("90.00") BigDecimal minCelsius,
        @NotNull @DecimalMin("-90.00") @DecimalMax("90.00") BigDecimal maxCelsius,
        @PositiveOrZero int maxSingleExcursionMinutes,
        @PositiveOrZero int maxCumulativeExcursionMinutes,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal minCoveragePercent) {
}
