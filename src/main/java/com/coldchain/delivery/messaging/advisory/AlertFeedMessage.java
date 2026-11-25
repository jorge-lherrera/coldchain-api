package com.coldchain.delivery.messaging.advisory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertFeedMessage(
        UUID excursionId,
        UUID shipmentId,
        UUID organizationId,
        String probableCause,
        String severity,
        BigDecimal confidence,
        String summaryEn,
        String summaryEs,
        String summaryPt,
        String recommendedAction,
        String source,
        String model,
        Boolean contextAvailable,
        Instant triagedAt) {
}
