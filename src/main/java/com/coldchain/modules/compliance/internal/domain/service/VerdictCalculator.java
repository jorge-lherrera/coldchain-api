package com.coldchain.modules.compliance.internal.domain.service;

import com.coldchain.modules.compliance.api.FindingCode;
import com.coldchain.modules.compliance.api.Severity;
import com.coldchain.modules.compliance.api.Verdict;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class VerdictCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private VerdictCalculator() {
    }

    public static Evaluation evaluate(EvaluationInput input) {
        List<FindingDraft> findings = new ArrayList<>();
        BigDecimal coverage = coverageOf(input);
        long cumulative = input.excursions().stream()
                .filter(ExcursionFact::closed)
                .mapToLong(ExcursionFact::durationMinutes)
                .sum();
        long longest = input.excursions().stream()
                .mapToLong(ExcursionFact::durationMinutes)
                .max()
                .orElse(0L);

        boolean failed = false;
        if (!input.deviceAssigned()) {
            findings.add(new FindingDraft(FindingCode.NO_DEVICE_ASSIGNED, Severity.CRITICAL, null,
                    "{\"readings\":0}"));
            failed = true;
        }
        if (coverage.compareTo(input.minCoveragePercent()) < 0) {
            findings.add(new FindingDraft(FindingCode.DATA_GAP, Severity.CRITICAL, null,
                    "{\"coveragePercent\":" + coverage + ",\"minimum\":"
                            + input.minCoveragePercent() + "}"));
            failed = true;
        }
        if (cumulative > input.maxCumulativeExcursionMinutes()
                || longest > input.maxSingleExcursionMinutes()) {
            failed = true;
        }
        for (ExcursionFact excursion : input.excursions()) {
            boolean breaches = excursion.durationMinutes() > input.maxSingleExcursionMinutes()
                    || cumulative > input.maxCumulativeExcursionMinutes();
            findings.add(new FindingDraft(
                    excursion.aboveMaximum() ? FindingCode.EXCURSION_ABOVE_MAX
                            : FindingCode.EXCURSION_BELOW_MIN,
                    breaches ? Severity.CRITICAL : Severity.WARNING,
                    excursion.excursionId(),
                    "{\"durationMinutes\":" + excursion.durationMinutes() + ",\"peakCelsius\":"
                            + excursion.peakCelsius() + "}"));
        }
        if (!input.calibrationValid()) {
            findings.add(new FindingDraft(FindingCode.CALIBRATION_EXPIRED, Severity.WARNING, null,
                    "{\"calibrationValid\":false}"));
        }

        Verdict verdict;
        if (failed) {
            verdict = Verdict.FAIL;
        } else if (!findings.isEmpty()) {
            verdict = Verdict.PASS_WITH_DEVIATION;
        } else {
            verdict = Verdict.PASS;
        }
        return new Evaluation(verdict, coverage, cumulative, longest, List.copyOf(findings));
    }

    private static BigDecimal coverageOf(EvaluationInput input) {
        long windowSeconds = Duration.between(input.evaluatedFrom(), input.evaluatedTo()).toSeconds();
        if (windowSeconds <= 0 || input.samplingIntervalSeconds() <= 0) {
            return BigDecimal.ZERO;
        }
        long expected = Math.max(1L, windowSeconds / input.samplingIntervalSeconds());
        BigDecimal ratio = BigDecimal.valueOf(input.readingsReceived())
                .divide(BigDecimal.valueOf(expected), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
        return ratio.min(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }
}
