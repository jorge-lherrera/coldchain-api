package com.coldchain.triage.internal.infrastructure.coldchainapi;

import com.coldchain.triage.internal.domain.model.ContextHandoff;
import com.coldchain.triage.internal.domain.model.ContextReading;
import com.coldchain.triage.internal.domain.model.CustodyParticipation;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.FrozenThresholds;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExcursionContextPayload(Instant windowFrom, Instant windowTo, List<Reading> readings,
        Thresholds thresholds, CustodyParticipation custodian, Long minutesSinceLastHandoff,
        List<Handoff> handoffs) {

    public ExcursionContext toContext() {
        return new ExcursionContext(windowFrom, windowTo,
                readings == null ? List.of() : readings.stream()
                        .map(reading -> new ContextReading(reading.measuredAt(), reading.celsius()))
                        .toList(),
                thresholds == null ? null : new FrozenThresholds(thresholds.minCelsius(),
                        thresholds.maxCelsius(), thresholds.maxSingleExcursionMinutes(),
                        thresholds.maxCumulativeExcursionMinutes()),
                custodian, minutesSinceLastHandoff,
                handoffs == null ? List.of() : handoffs.stream()
                        .map(handoff -> new ContextHandoff(handoff.occurredAt(), handoff.from(), handoff.to()))
                        .toList());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reading(Instant measuredAt, BigDecimal celsius) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thresholds(BigDecimal minCelsius, BigDecimal maxCelsius, int maxSingleExcursionMinutes,
            int maxCumulativeExcursionMinutes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Handoff(Instant occurredAt, CustodyParticipation from, CustodyParticipation to) {
    }
}
