package com.coldchain.triage.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record ContextReading(Instant measuredAt, BigDecimal celsius) {

    public ContextReading {
        Objects.requireNonNull(measuredAt);
        Objects.requireNonNull(celsius);
    }
}
