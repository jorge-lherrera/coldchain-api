package com.coldchain.modules.compliance.internal.domain.service;

import com.coldchain.modules.compliance.api.Verdict;
import java.math.BigDecimal;
import java.util.List;

public record Evaluation(
        Verdict verdict,
        BigDecimal coveragePercent,
        long cumulativeExcursionMinutes,
        long longestExcursionMinutes,
        List<FindingDraft> findings) {
}
