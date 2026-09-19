package com.coldchain.modules.compliance.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.compliance.api.FindingCode;
import com.coldchain.modules.compliance.api.Severity;
import com.coldchain.modules.compliance.api.Verdict;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerdictCalculatorTest {

    private static final Instant DEPARTURE = Instant.parse("2026-04-01T08:00:00Z");

    private static final Instant ARRIVAL = DEPARTURE.plus(3, ChronoUnit.HOURS);

    private static final int EVERY_FIVE_MINUTES = 300;

    private static final int A_FULL_SERIES = 36;

    @Test
    void aJourneyInBandAndFullyCoveredPasses() {
        Evaluation evaluation = VerdictCalculator.evaluate(input(A_FULL_SERIES, List.of()));

        assertThat(evaluation.verdict()).isEqualTo(Verdict.PASS);
        assertThat(evaluation.findings()).isEmpty();
        assertThat(evaluation.coveragePercent()).isEqualByComparingTo("100.00");
        assertThat(evaluation.cumulativeExcursionMinutes()).isZero();
    }

    @Test
    void coverageIsTheSamplesReceivedOverTheSamplesTheDeviceOwed() {
        Evaluation half = VerdictCalculator.evaluate(input(18, List.of()));

        assertThat(half.coveragePercent())
                .describedAs("eighteen of the thirty-six samples a five-minute device owes over "
                        + "three hours is half the journey unmeasured")
                .isEqualByComparingTo("50.00");
        assertThat(half.verdict()).isEqualTo(Verdict.FAIL);
        assertThat(codes(half)).containsExactly(FindingCode.DATA_GAP);
    }

    @Test
    void measuringNothingIsNotComplying() {
        Evaluation nothing = VerdictCalculator.evaluate(input(0, List.of()));

        assertThat(nothing.coveragePercent()).isEqualByComparingTo("0.00");
        assertThat(nothing.verdict())
                .describedAs("turning the sensor off cannot be the cheapest way to pass")
                .isEqualTo(Verdict.FAIL);
    }

    @Test
    void aShortExcursionIsADeviationAndNotAFailure() {
        Evaluation evaluation = VerdictCalculator.evaluate(
                input(A_FULL_SERIES, List.of(excursion(20, true))));

        assertThat(evaluation.verdict())
                .describedAs("twenty minutes above the maximum is inside the thirty the profile "
                        + "allows, and a product that is still good must not be thrown away")
                .isEqualTo(Verdict.PASS_WITH_DEVIATION);
        assertThat(codes(evaluation)).containsExactly(FindingCode.EXCURSION_ABOVE_MAX);
        assertThat(evaluation.findings().getFirst().severity()).isEqualTo(Severity.WARNING);
        assertThat(evaluation.longestExcursionMinutes()).isEqualTo(20);
    }

    @Test
    void oneExcursionLongerThanTheProfileAllowsFails() {
        Evaluation evaluation = VerdictCalculator.evaluate(
                input(A_FULL_SERIES, List.of(excursion(31, true))));

        assertThat(evaluation.verdict()).isEqualTo(Verdict.FAIL);
        assertThat(evaluation.findings().getFirst().severity()).isEqualTo(Severity.CRITICAL);
        assertThat(evaluation.longestExcursionMinutes()).isEqualTo(31);
    }

    @Test
    void severalShortExcursionsThatAddUpPastTheAllowanceAlsoFail() {
        Evaluation evaluation = VerdictCalculator.evaluate(input(A_FULL_SERIES,
                List.of(excursion(25, true), excursion(25, true), excursion(25, true),
                        excursion(25, true), excursion(25, true))));

        assertThat(evaluation.cumulativeExcursionMinutes())
                .describedAs("five excursions of twenty-five minutes are a hundred and twenty-five, "
                        + "past the hundred and twenty the profile allows in total")
                .isEqualTo(125);
        assertThat(evaluation.longestExcursionMinutes())
                .describedAs("no single one of them breaks the single-excursion limit")
                .isEqualTo(25);
        assertThat(evaluation.verdict()).isEqualTo(Verdict.FAIL);
        assertThat(evaluation.findings()).allSatisfy(finding ->
                assertThat(finding.severity()).isEqualTo(Severity.CRITICAL));
    }

    @Test
    void anExcursionStillOpenAtTheEndCountsTowardsTheLongestButNotTheTotal() {
        Evaluation evaluation = VerdictCalculator.evaluate(
                input(A_FULL_SERIES, List.of(excursion(40, true, false))));

        assertThat(evaluation.cumulativeExcursionMinutes())
                .describedAs("an excursion nobody saw end has no duration to add up yet")
                .isZero();
        assertThat(evaluation.longestExcursionMinutes()).isEqualTo(40);
        assertThat(evaluation.verdict())
                .describedAs("it is already longer than a single excursion may be")
                .isEqualTo(Verdict.FAIL);
    }

    @Test
    void aJourneyWithNoDeviceFailsBeforeAnythingElseIsLookedAt() {
        Evaluation evaluation = VerdictCalculator.evaluate(new EvaluationInput(
                new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120, new BigDecimal("80.00"),
                DEPARTURE, ARRIVAL, EVERY_FIVE_MINUTES, 0, true, false, List.of()));

        assertThat(evaluation.verdict()).isEqualTo(Verdict.FAIL);
        assertThat(codes(evaluation)).contains(FindingCode.NO_DEVICE_ASSIGNED);
    }

    @Test
    void anExpiredCalibrationIsNotedWithoutCondemningTheJourney() {
        Evaluation evaluation = VerdictCalculator.evaluate(new EvaluationInput(
                new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120, new BigDecimal("80.00"),
                DEPARTURE, ARRIVAL, EVERY_FIVE_MINUTES, A_FULL_SERIES, false, true, List.of()));

        assertThat(evaluation.verdict())
                .describedAs("a sensor overdue for calibration measured something, and something is "
                        + "not nothing")
                .isEqualTo(Verdict.PASS_WITH_DEVIATION);
        assertThat(codes(evaluation)).containsExactly(FindingCode.CALIBRATION_EXPIRED);
    }

    @Test
    void anExcursionBelowTheMinimumIsToldApartFromOneAbove() {
        Evaluation evaluation = VerdictCalculator.evaluate(
                input(A_FULL_SERIES, List.of(excursion(10, false))));

        assertThat(codes(evaluation)).containsExactly(FindingCode.EXCURSION_BELOW_MIN);
    }

    @Test
    void coverageNeverReadsAboveWhatWasAsked() {
        Evaluation evaluation = VerdictCalculator.evaluate(input(A_FULL_SERIES * 3, List.of()));

        assertThat(evaluation.coveragePercent())
                .describedAs("a device that reported three times as often did not cover three "
                        + "hundred percent of the journey")
                .isEqualByComparingTo("100.00");
    }

    private EvaluationInput input(int readings, List<ExcursionFact> excursions) {
        return new EvaluationInput(new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120,
                new BigDecimal("80.00"), DEPARTURE, ARRIVAL, EVERY_FIVE_MINUTES, readings, true,
                true, excursions);
    }

    private ExcursionFact excursion(long minutes, boolean aboveMaximum) {
        return excursion(minutes, aboveMaximum, true);
    }

    private ExcursionFact excursion(long minutes, boolean aboveMaximum, boolean closed) {
        return new ExcursionFact(UUID.randomUUID(), aboveMaximum, closed, minutes,
                aboveMaximum ? new BigDecimal("11.50") : new BigDecimal("0.50"));
    }

    private List<FindingCode> codes(Evaluation evaluation) {
        return evaluation.findings().stream().map(FindingDraft::code).toList();
    }
}
