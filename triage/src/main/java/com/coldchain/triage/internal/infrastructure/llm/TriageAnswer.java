package com.coldchain.triage.internal.infrastructure.llm;

import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.api.TriageSource;
import com.coldchain.triage.internal.domain.model.Summary;
import com.coldchain.triage.internal.domain.model.Triage;
import com.coldchain.triage.internal.domain.model.TriageProvenance;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/**
 * What the model answered, read as it came. The enums are strings on purpose: an answer outside the
 * closed sets has to reach {@link #flaw()} to be named and discarded, never be coerced or repaired.
 */
record TriageAnswer(String probableCause, String severity, BigDecimal confidence, Summaries summary,
        String recommendedAction) {

    private static final int CONFIDENCE_SCALE = 2;

    record Summaries(String en, String es, String pt) {
    }

    Optional<String> flaw() {
        if (!isOneOf(probableCause, ProbableCause.values())) {
            return Optional.of("a cause outside the set: " + probableCause);
        }
        if (!isOneOf(severity, Severity.values())) {
            return Optional.of("a severity outside the set: " + severity);
        }
        if (confidence == null || confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
            return Optional.of("a confidence outside 0 to 1: " + confidence);
        }
        if (summary == null || !written(summary.en(), Summary.MAX_LENGTH) || !written(summary.es(), Summary.MAX_LENGTH)
                || !written(summary.pt(), Summary.MAX_LENGTH)) {
            return Optional.of("a summary missing, or too long, in one of the three languages");
        }
        if (!written(recommendedAction, Triage.MAX_ACTION_LENGTH)) {
            return Optional.of("a recommended action missing, or too long");
        }
        return Optional.empty();
    }

    Triage toTriage(TriageProvenance provenance, Instant triagedAt) {
        flaw().ifPresent(found -> {
            throw new IllegalStateException("An answer with " + found + " is discarded, never turned into a triage");
        });
        return new Triage(ProbableCause.valueOf(probableCause), Severity.valueOf(severity),
                confidence.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP),
                new Summary(summary.en(), summary.es(), summary.pt()), recommendedAction, TriageSource.LLM,
                provenance, triagedAt);
    }

    private static boolean isOneOf(String value, Enum<?>[] allowed) {
        return value != null && Arrays.stream(allowed).map(Enum::name).anyMatch(value::equals);
    }

    private static boolean written(String text, int maxLength) {
        return text != null && !text.isBlank() && text.length() <= maxLength;
    }
}
