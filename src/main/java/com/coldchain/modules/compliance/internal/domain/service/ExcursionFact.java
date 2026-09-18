package com.coldchain.modules.compliance.internal.domain.service;

import java.math.BigDecimal;
import java.util.UUID;

public record ExcursionFact(UUID excursionId, boolean aboveMaximum, boolean closed,
        long durationMinutes, BigDecimal peakCelsius) {
}
