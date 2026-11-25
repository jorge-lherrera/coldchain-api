package com.coldchain.triage.internal.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ExcursionContext(Instant windowFrom, Instant windowTo, List<ContextReading> readings,
        FrozenThresholds thresholds, CustodyParticipation custodian, Long minutesSinceLastHandoff,
        List<ContextHandoff> handoffs) {

    public ExcursionContext {
        Objects.requireNonNull(windowFrom);
        Objects.requireNonNull(windowTo);
        Objects.requireNonNull(custodian);
        readings = List.copyOf(readings);
        handoffs = List.copyOf(handoffs);
    }

    public Optional<FrozenThresholds> frozenThresholds() {
        return Optional.ofNullable(thresholds);
    }
}
