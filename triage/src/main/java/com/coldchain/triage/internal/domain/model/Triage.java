package com.coldchain.triage.internal.domain.model;

import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.api.TriageSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Triage(ProbableCause probableCause, Severity severity, BigDecimal confidence, Summary summary,
        String recommendedAction, TriageSource source, TriageProvenance provenance, Instant triagedAt) {

    public static final int MAX_ACTION_LENGTH = 500;

    public Triage {
        Objects.requireNonNull(probableCause);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(summary);
        Objects.requireNonNull(recommendedAction);
        Objects.requireNonNull(source);
        Objects.requireNonNull(provenance);
        Objects.requireNonNull(triagedAt);
        if (recommendedAction.isBlank() || recommendedAction.length() > MAX_ACTION_LENGTH) {
            throw new IllegalArgumentException("A recommended action is one instruction, at most "
                    + MAX_ACTION_LENGTH + " characters, received " + recommendedAction.length());
        }
        if (confidence != null && (confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("A confidence lies between 0 and 1, received " + confidence);
        }
        if ((source == TriageSource.LLM) != (provenance.model() != null)) {
            throw new IllegalArgumentException("A model is named exactly when a model answered");
        }
    }

    public boolean byModel() {
        return source == TriageSource.LLM;
    }
}
