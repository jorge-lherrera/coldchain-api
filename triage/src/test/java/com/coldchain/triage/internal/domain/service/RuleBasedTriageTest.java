package com.coldchain.triage.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.triage.api.ExcursionEventType;
import com.coldchain.triage.api.ExcursionKind;
import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.api.TriageSource;
import com.coldchain.triage.internal.domain.model.CustodyParticipation;
import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.ExcursionEvent;
import com.coldchain.triage.internal.domain.model.FrozenThresholds;
import com.coldchain.triage.internal.domain.model.Triage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RuleBasedTriageTest {

    private static final UUID SHIPMENT = UUID.fromString("01930000-0000-7000-8000-00000000000a");

    private static final UUID ORGANIZATION = UUID.fromString("01930000-0000-7000-8000-00000000000b");

    private static final UUID EXCURSION = UUID.fromString("01930000-0000-7000-8000-00000000000c");

    private static final Instant OPENED = Instant.parse("2026-05-01T10:05:00Z");

    private static final Instant NOW = Instant.parse("2026-05-01T11:00:00Z");

    @Test
    void anOpenExcursionIsMediumUntilItsLengthIsKnown() {
        Triage triage = RuleBasedTriage.of(open(ExcursionKind.ABOVE_MAX, "9.50"), NOW);

        assertThat(triage.severity()).isEqualTo(Severity.MEDIUM);
    }

    @ParameterizedTest
    @CsvSource({"0, LOW", "14, LOW", "15, MEDIUM", "29, MEDIUM", "30, HIGH", "240, HIGH"})
    void aClosedExcursionIsAsSevereAsItWasLong(long minutes, Severity expected) {
        Triage triage = RuleBasedTriage.of(closed(ExcursionKind.ABOVE_MAX, "9.50", minutes), NOW);

        assertThat(triage.severity()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"29, LOW", "30, MEDIUM", "59, MEDIUM", "60, HIGH"})
    void withTheContextTheLengthIsMeasuredAgainstWhatTheShipmentAllows(long minutes, Severity expected) {
        Triage triage = RuleBasedTriage.of(closed(ExcursionKind.ABOVE_MAX, "9.50", minutes),
                context(new FrozenThresholds(new BigDecimal("2.00"), new BigDecimal("8.00"), 60, 120)), NOW);

        assertThat(triage.severity())
                .describedAs("a profile that tolerates an hour out of range makes forty minutes a warning")
                .isEqualTo(expected);
        assertThat(triage.provenance().contextAvailable()).isTrue();
    }

    @Test
    void aContextWithoutThresholdsFallsBackToTheDefaultAllowance() {
        Triage triage = RuleBasedTriage.of(closed(ExcursionKind.ABOVE_MAX, "9.50", 45), context(null), NOW);

        assertThat(triage.severity()).isEqualTo(Severity.HIGH);
        assertThat(triage.provenance().contextAvailable()).isTrue();
    }

    @Test
    void withoutTheContextTheTriageSaysItHadNone() {
        Triage triage = RuleBasedTriage.of(closed(ExcursionKind.ABOVE_MAX, "9.50", 45), NOW);

        assertThat(triage.provenance().contextAvailable()).isFalse();
    }

    @Test
    void theRulesNameNoCauseAndNoModel() {
        Triage triage = RuleBasedTriage.of(closed(ExcursionKind.ABOVE_MAX, "11.50", 45), NOW);

        assertThat(triage.probableCause()).isEqualTo(ProbableCause.UNKNOWN);
        assertThat(triage.source()).isEqualTo(TriageSource.RULES);
        assertThat(triage.provenance().model()).isNull();
        assertThat(triage.confidence())
                .describedAs("rules give no probability, and inventing one would read as a model's answer")
                .isNull();
        assertThat(triage.triagedAt()).isEqualTo(NOW);
    }

    @Test
    void theSummaryIsWrittenInEachLanguageWithItsOwnDecimalSeparator() {
        Triage triage = RuleBasedTriage.of(closed(ExcursionKind.ABOVE_MAX, "11.50", 45), NOW);

        assertThat(triage.summary().english())
                .isEqualTo("The temperature stayed above the maximum for 45 minutes. Highest reading: 11.5 °C.");
        assertThat(triage.summary().spanish())
                .isEqualTo("La temperatura estuvo por encima del máximo durante 45 minutos. Lectura más alta: 11,5 °C.");
        assertThat(triage.summary().portuguese())
                .isEqualTo("A temperatura ficou acima do máximo por 45 minutos. Leitura mais alta: 11,5 °C.");
    }

    @Test
    void anExcursionBelowTheMinimumSaysSo() {
        Triage triage = RuleBasedTriage.of(open(ExcursionKind.BELOW_MIN, "0.80"), NOW);

        assertThat(triage.summary().english()).isEqualTo(
                "The temperature fell below the minimum and is still out of range. Lowest reading so far: 0.8 °C.");
    }

    @Test
    void aCloseHeardBeforeItsOpenIsSummarisedWithoutAPeak() {
        ExcursionAlert alert = ExcursionAlert.firstHeardFrom(new ExcursionEvent(UUID.randomUUID(),
                ExcursionEventType.CLOSED, NOW, SHIPMENT, ORGANIZATION, EXCURSION, ExcursionKind.ABOVE_MAX, null, 1L));

        assertThat(RuleBasedTriage.of(alert, NOW).summary().english())
                .isEqualTo("The temperature stayed above the maximum for 1 minute.");
    }

    @Test
    void theSummaryNamesNoIdentifier() {
        Triage triage = RuleBasedTriage.of(closed(ExcursionKind.ABOVE_MAX, "11.50", 45), NOW);

        List<String> summaries = List.of(triage.summary().english(), triage.summary().spanish(),
                triage.summary().portuguese(), triage.recommendedAction());
        assertThat(summaries).allSatisfy(text -> assertThat(text)
                .doesNotContain(SHIPMENT.toString(), ORGANIZATION.toString(), EXCURSION.toString()));
    }

    private static ExcursionContext context(FrozenThresholds thresholds) {
        return new ExcursionContext(OPENED.minusSeconds(1800), NOW, List.of(), thresholds,
                CustodyParticipation.CARRIER, 25L, List.of());
    }

    private static ExcursionAlert open(ExcursionKind kind, String peak) {
        return ExcursionAlert.firstHeardFrom(new ExcursionEvent(UUID.randomUUID(), ExcursionEventType.OPENED,
                OPENED, SHIPMENT, ORGANIZATION, EXCURSION, kind, new BigDecimal(peak), null));
    }

    private static ExcursionAlert closed(ExcursionKind kind, String peak, long minutes) {
        return open(kind, peak).apply(new ExcursionEvent(UUID.randomUUID(), ExcursionEventType.CLOSED,
                OPENED.plusSeconds(minutes * 60), SHIPMENT, ORGANIZATION, EXCURSION, kind, null, minutes));
    }
}
