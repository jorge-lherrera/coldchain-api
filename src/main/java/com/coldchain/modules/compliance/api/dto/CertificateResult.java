package com.coldchain.modules.compliance.api.dto;

import com.coldchain.modules.compliance.api.Verdict;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CertificateResult(
        UUID id,
        UUID shipmentId,
        int version,
        Verdict verdict,
        BigDecimal coveragePercent,
        long cumulativeExcursionMinutes,
        long longestExcursionMinutes,
        Instant evaluatedFrom,
        Instant evaluatedTo,
        Instant issuedAt,
        String contentHash,
        List<FindingResult> findings) {
}
