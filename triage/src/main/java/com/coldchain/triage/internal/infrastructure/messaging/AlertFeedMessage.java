package com.coldchain.triage.internal.infrastructure.messaging;

import com.coldchain.triage.api.event.AlertTriaged;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The alert as it travels to the API. A wire shape of its own, so that renaming a field of the event
 * is a decision about the contract rather than an accident of refactoring (ADR-010).
 */
record AlertFeedMessage(UUID excursionId, UUID shipmentId, UUID organizationId, String probableCause,
        String severity, BigDecimal confidence, String summaryEn, String summaryEs, String summaryPt,
        String recommendedAction, String source, String model, boolean contextAvailable, Instant triagedAt) {

    static AlertFeedMessage of(AlertTriaged alert) {
        return new AlertFeedMessage(alert.excursionId(), alert.shipmentId(), alert.organizationId(),
                alert.probableCause().name(), alert.severity().name(), alert.confidence(), alert.summaryEn(),
                alert.summaryEs(), alert.summaryPt(), alert.recommendedAction(), alert.source().name(),
                alert.model(), alert.contextAvailable(), alert.triagedAt());
    }
}
