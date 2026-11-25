package com.coldchain.triage.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.triage.api.ExcursionEventType;
import com.coldchain.triage.api.ExcursionKind;
import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.api.TriageSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExcursionAlertTest {

    private static final UUID SHIPMENT = UUID.fromString("01930000-0000-7000-8000-000000000001");

    private static final UUID ORGANIZATION = UUID.fromString("01930000-0000-7000-8000-000000000002");

    private static final UUID EXCURSION = UUID.fromString("01930000-0000-7000-8000-000000000003");

    private static final Instant OPENED = Instant.parse("2026-05-01T10:05:00Z");

    private static final Instant CLOSED = Instant.parse("2026-05-01T10:50:00Z");

    @Test
    void anOpenedExcursionIsAnOpenAlertWithItsPeak() {
        ExcursionAlert alert = ExcursionAlert.firstHeardFrom(opened());

        assertThat(alert.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.openedAt()).isEqualTo(OPENED);
        assertThat(alert.peakCelsius()).isEqualByComparingTo("9.50");
        assertThat(alert.closedAt()).isNull();
        assertThat(alert.durationMinutes()).isNull();
        assertThat(alert.version()).describedAs("an alert nobody stored yet has no version").isNull();
    }

    @Test
    void aCloseFinishesTheAlertWithItsDuration() {
        ExcursionAlert alert = ExcursionAlert.firstHeardFrom(opened()).apply(closed());

        assertThat(alert.status()).isEqualTo(AlertStatus.CLOSED);
        assertThat(alert.closedAt()).isEqualTo(CLOSED);
        assertThat(alert.durationMinutes()).isEqualTo(45);
        assertThat(alert.peakCelsius()).isEqualByComparingTo("9.50");
    }

    @Test
    void aCloseHeardBeforeItsOpenIsAClosedAlert() {
        ExcursionAlert alert = ExcursionAlert.firstHeardFrom(closed());

        assertThat(alert.status())
                .describedAs("Kafka can deliver a retried open after its close, and the close is still a fact")
                .isEqualTo(AlertStatus.CLOSED);
        assertThat(alert.durationMinutes()).isEqualTo(45);
        assertThat(alert.openedAt()).isNull();
        assertThat(alert.peakCelsius()).isNull();
    }

    @Test
    void theLateOpenOnlyFillsInWhatWasMissing() {
        ExcursionAlert alert = ExcursionAlert.firstHeardFrom(closed()).apply(opened());

        assertThat(alert.status())
                .describedAs("an open that happened before the close cannot reopen it")
                .isEqualTo(AlertStatus.CLOSED);
        assertThat(alert.openedAt()).isEqualTo(OPENED);
        assertThat(alert.peakCelsius()).isEqualByComparingTo("9.50");
        assertThat(alert.closedAt()).isEqualTo(CLOSED);
        assertThat(alert.durationMinutes()).isEqualTo(45);
    }

    @Test
    void theRulesNeverOverwriteWhatTheModelAnswered() {
        Triage byModel = new Triage(ProbableCause.DOOR_OPENING, Severity.LOW, new BigDecimal("0.80"),
                new Summary("en", "es", "pt"), "Close the door.", TriageSource.LLM,
                new TriageProvenance("claude-opus-5-5", "triage-v1", 900, 120, 2400, true), CLOSED);
        Triage byRules = new Triage(ProbableCause.UNKNOWN, Severity.HIGH, null, new Summary("en", "es", "pt"),
                "Check it.", TriageSource.RULES, TriageProvenance.ofRules(false), CLOSED.plusSeconds(5));

        ExcursionAlert alert = ExcursionAlert.firstHeardFrom(closed()).triagedBy(byModel).triagedBy(byRules);

        assertThat(alert.triage())
                .describedAs("a late open recomputes the rules, and that must not erase the model's answer")
                .isEqualTo(byModel);
    }

    @Test
    void anEventOfAnotherExcursionIsRefused() {
        ExcursionAlert alert = ExcursionAlert.firstHeardFrom(opened());
        ExcursionEvent stranger = new ExcursionEvent(UUID.randomUUID(), ExcursionEventType.CLOSED, CLOSED,
                SHIPMENT, ORGANIZATION, UUID.randomUUID(), ExcursionKind.ABOVE_MAX, null, 45L);

        assertThatThrownBy(() -> alert.apply(stranger)).isInstanceOf(IllegalArgumentException.class);
    }

    private static ExcursionEvent opened() {
        return new ExcursionEvent(UUID.randomUUID(), ExcursionEventType.OPENED, OPENED, SHIPMENT, ORGANIZATION,
                EXCURSION, ExcursionKind.ABOVE_MAX, new BigDecimal("9.50"), null);
    }

    private static ExcursionEvent closed() {
        return new ExcursionEvent(UUID.randomUUID(), ExcursionEventType.CLOSED, CLOSED, SHIPMENT, ORGANIZATION,
                EXCURSION, ExcursionKind.ABOVE_MAX, null, 45L);
    }
}
