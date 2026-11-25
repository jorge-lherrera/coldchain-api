package com.coldchain.modules.advisory.api.dto;

import com.coldchain.modules.advisory.api.AlertSeverity;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExcursionAlertResult(
        UUID id,
        UUID shipmentId,
        UUID excursionId,
        ProbableCause probableCause,
        AlertSeverity severity,
        BigDecimal confidence,
        String summaryEn,
        String summaryEs,
        String summaryPt,
        String recommendedAction,
        AlertSource source,
        String model,
        boolean contextAvailable,
        Instant triagedAt) {
}
