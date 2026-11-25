package com.coldchain.triage.api.event;

import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.api.TriageSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertTriaged(UUID excursionId, UUID shipmentId, UUID organizationId, ProbableCause probableCause,
        Severity severity, BigDecimal confidence, String summaryEn, String summaryEs, String summaryPt,
        String recommendedAction, TriageSource source, String model, boolean contextAvailable,
        Instant triagedAt) {
}
