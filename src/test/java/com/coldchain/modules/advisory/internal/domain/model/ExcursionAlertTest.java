package com.coldchain.modules.advisory.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.modules.advisory.api.AlertSeverity;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExcursionAlertTest {

    private static final Instant OPENED = Instant.parse("2026-05-01T10:05:00Z");

    private static final Instant CLOSED = Instant.parse("2026-05-01T10:50:00Z");

    private final ExcursionAlert fromTheRules = ExcursionAlert.createNew(UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), rules(OPENED));

    @Test
    void aNewerTriageReplacesTheAdvice() {
        Advice model = model(CLOSED);

        ExcursionAlert reconsidered = fromTheRules.reconsideredWith(model);

        assertThat(fromTheRules.outdatedBy(model)).isTrue();
        assertThat(reconsidered.advice()).isEqualTo(model);
        assertThat(reconsidered.id()).isEqualTo(fromTheRules.id());
        assertThat(reconsidered.excursionId()).isEqualTo(fromTheRules.excursionId());
    }

    @Test
    void anOlderTriageChangesNothing() {
        ExcursionAlert latest = fromTheRules.reconsideredWith(model(CLOSED));

        ExcursionAlert redelivered = latest.reconsideredWith(rules(OPENED));

        assertThat(latest.outdatedBy(rules(OPENED)))
                .describedAs("Kafka may deliver the open's triage again after the close's")
                .isFalse();
        assertThat(redelivered).isSameAs(latest);
        assertThat(redelivered.advice().source()).isEqualTo(AlertSource.LLM);
    }

    @Test
    void theSameTriageTwiceChangesNothing() {
        ExcursionAlert again = fromTheRules.reconsideredWith(rules(OPENED));

        assertThat(fromTheRules.outdatedBy(rules(OPENED))).isFalse();
        assertThat(again).isSameAs(fromTheRules);
    }

    @Test
    void aModelIsNamedExactlyWhenAModelAnswered() {
        assertThatThrownBy(() -> new Advice(ProbableCause.UNKNOWN, AlertSeverity.LOW, null, "en", "es", "pt",
                "Check it.", AlertSource.RULES, "claude-opus-5-5", true, OPENED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Advice(ProbableCause.DOOR_OPENING, AlertSeverity.LOW, new BigDecimal("0.8"),
                "en", "es", "pt", "Check it.", AlertSource.LLM, null, true, OPENED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aConfidenceLiesBetweenZeroAndOne() {
        assertThatThrownBy(() -> new Advice(ProbableCause.DOOR_OPENING, AlertSeverity.LOW, new BigDecimal("1.20"),
                "en", "es", "pt", "Check it.", AlertSource.LLM, "claude-opus-5-5", true, CLOSED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Advice rules(Instant triagedAt) {
        return new Advice(ProbableCause.UNKNOWN, AlertSeverity.MEDIUM, null, "Still out of range.",
                "Sigue fuera de rango.", "Continua fora da faixa.", "Check the shipment now.", AlertSource.RULES,
                null, false, triagedAt);
    }

    private static Advice model(Instant triagedAt) {
        return new Advice(ProbableCause.DOOR_OPENING, AlertSeverity.LOW, new BigDecimal("0.80"),
                "A short spike.", "Un pico corto.", "Um pico curto.", "Keep the doors shut.", AlertSource.LLM,
                "claude-opus-5-5", true, triagedAt);
    }
}
