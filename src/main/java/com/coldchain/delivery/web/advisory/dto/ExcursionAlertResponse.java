package com.coldchain.delivery.web.advisory.dto;

import com.coldchain.modules.advisory.api.AlertSeverity;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExcursionAlertResponse(
        UUID id,
        UUID excursionId,
        ProbableCause probableCause,
        BigDecimal confidence,
        AlertSource source,
        AlertSeverity severity,
        String summary,
        String recommendedAction,
        String model,
        boolean contextAvailable,
        Instant triagedAt) {
}
