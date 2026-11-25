package com.coldchain.triage.internal.application.mapper;

import com.coldchain.triage.api.event.AlertTriaged;
import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.Triage;

public final class AlertTriagedMapper {

    private AlertTriagedMapper() {
    }

    public static AlertTriaged toEvent(ExcursionAlert alert) {
        Triage triage = alert.triage();
        if (triage == null) {
            throw new IllegalStateException("Excursion " + alert.excursionId() + " has no triage to publish");
        }
        return new AlertTriaged(alert.excursionId(), alert.shipmentId(), alert.organizationId(),
                triage.probableCause(), triage.severity(), triage.confidence(), triage.summary().english(),
                triage.summary().spanish(), triage.summary().portuguese(), triage.recommendedAction(),
                triage.source(), triage.provenance().model(), triage.provenance().contextAvailable(),
                triage.triagedAt());
    }
}
