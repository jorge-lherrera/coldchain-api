package com.coldchain.modules.advisory.internal.domain.model;

import com.coldchain.modules.advisory.api.AlertSeverity;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Advice(
        ProbableCause probableCause,
        AlertSeverity severity,
        BigDecimal confidence,
        String summaryEn,
        String summaryEs,
        String summaryPt,
        String recommendedAction,
        AlertSource source,
        String model,
        boolean contextAvailable,
        Instant triagedAt) {

    public Advice {
        Objects.requireNonNull(probableCause);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(source);
        Objects.requireNonNull(triagedAt);
        requireText(summaryEn);
        requireText(summaryEs);
        requireText(summaryPt);
        requireText(recommendedAction);
        if (confidence != null && (confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("A confidence lies between 0 and 1");
        }
        if ((source == AlertSource.LLM) != (model != null)) {
            throw new IllegalArgumentException("A model is named exactly when a model answered");
        }
    }

    public boolean newerThan(Advice other) {
        return triagedAt.isAfter(other.triagedAt);
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Advice comes with its summaries and its recommended action");
        }
    }
}
