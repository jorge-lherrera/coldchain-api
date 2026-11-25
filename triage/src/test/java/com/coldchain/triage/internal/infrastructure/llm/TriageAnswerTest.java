package com.coldchain.triage.internal.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.api.TriageSource;
import com.coldchain.triage.internal.domain.model.Triage;
import com.coldchain.triage.internal.domain.model.TriageProvenance;
import com.coldchain.triage.internal.infrastructure.llm.TriageAnswer.Summaries;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TriageAnswerTest {

    private static final Instant NOW = Instant.parse("2026-05-01T11:00:00Z");

    private static final TriageProvenance PROVENANCE =
            new TriageProvenance("claude-opus-5-5", TriagePrompt.VERSION, 1200, 300, 900, true);

    private static final Summaries SUMMARIES = new Summaries("The door was opened.", "Se abrió la puerta.",
            "A porta foi aberta.");

    @Test
    void anAnswerInsideTheSchemaBecomesATriageByTheModel() {
        TriageAnswer answer = new TriageAnswer("DOOR_OPENING", "LOW", new BigDecimal("0.825"), SUMMARIES,
                "Keep the doors shut during loading.");

        Triage triage = answer.toTriage(PROVENANCE, NOW);

        assertThat(answer.flaw()).isEmpty();
        assertThat(triage.source()).isEqualTo(TriageSource.LLM);
        assertThat(triage.probableCause()).isEqualTo(ProbableCause.DOOR_OPENING);
        assertThat(triage.severity()).isEqualTo(Severity.LOW);
        assertThat(triage.confidence()).isEqualByComparingTo("0.83");
        assertThat(triage.summary().portuguese()).isEqualTo("A porta foi aberta.");
        assertThat(triage.provenance()).isEqualTo(PROVENANCE);
    }

    @Test
    void aCauseOutsideTheSetIsAFlaw() {
        TriageAnswer answer = new TriageAnswer("COMPRESSOR_FAILURE", "HIGH", new BigDecimal("0.9"), SUMMARIES,
                "Check the unit.");

        assertThat(answer.flaw()).hasValueSatisfying(flaw -> assertThat(flaw).contains("COMPRESSOR_FAILURE"));
    }

    @Test
    void aSeverityIsOneOfTheThreeAsWritten() {
        TriageAnswer answer = new TriageAnswer("DOOR_OPENING", "high", new BigDecimal("0.9"), SUMMARIES,
                "Check the unit.");

        assertThat(answer.flaw())
                .describedAs("an answer outside the schema is discarded, never repaired")
                .hasValueSatisfying(flaw -> assertThat(flaw).contains("severity"));
    }

    @Test
    void aConfidenceAboveOneIsAFlaw() {
        TriageAnswer answer = new TriageAnswer("DOOR_OPENING", "LOW", new BigDecimal("1.2"), SUMMARIES,
                "Check the unit.");

        assertThat(answer.flaw()).hasValueSatisfying(flaw -> assertThat(flaw).contains("1.2"));
    }

    @Test
    void aMissingConfidenceIsAFlaw() {
        TriageAnswer answer = new TriageAnswer("DOOR_OPENING", "LOW", null, SUMMARIES, "Check the unit.");

        assertThat(answer.flaw()).hasValueSatisfying(flaw -> assertThat(flaw).contains("confidence"));
    }

    @Test
    void aSummaryMissingInOneLanguageIsAFlaw() {
        TriageAnswer answer = new TriageAnswer("DOOR_OPENING", "LOW", new BigDecimal("0.7"),
                new Summaries("The door was opened.", "Se abrió la puerta.", " "), "Check the unit.");

        assertThat(answer.flaw()).hasValueSatisfying(flaw -> assertThat(flaw).contains("summary"));
    }

    @Test
    void aSummaryLongerThanTheAlertHoldsIsAFlaw() {
        TriageAnswer answer = new TriageAnswer("DOOR_OPENING", "LOW", new BigDecimal("0.7"),
                new Summaries("x".repeat(1001), "Se abrió la puerta.", "A porta foi aberta."), "Check the unit.");

        assertThat(answer.flaw())
                .describedAs("a reply that cannot be stored would fail the transaction, so it never reaches it")
                .hasValueSatisfying(flaw -> assertThat(flaw).contains("too long"));
    }

    @Test
    void aMissingRecommendedActionIsAFlaw() {
        TriageAnswer answer = new TriageAnswer("DOOR_OPENING", "LOW", new BigDecimal("0.7"), SUMMARIES, null);

        assertThat(answer.flaw()).hasValueSatisfying(flaw -> assertThat(flaw).contains("recommended action"));
    }

    @Test
    void aFlawedAnswerIsNeverTurnedIntoATriage() {
        TriageAnswer answer = new TriageAnswer("DOOR_OPENING", "LOW", new BigDecimal("1.2"), SUMMARIES,
                "Check the unit.");

        assertThatThrownBy(() -> answer.toTriage(PROVENANCE, NOW)).isInstanceOf(IllegalStateException.class);
    }
}
