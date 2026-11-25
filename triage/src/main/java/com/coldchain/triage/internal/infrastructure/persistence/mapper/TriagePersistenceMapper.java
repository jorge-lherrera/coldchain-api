package com.coldchain.triage.internal.infrastructure.persistence.mapper;

import com.coldchain.triage.api.ExcursionEventType;
import com.coldchain.triage.api.ExcursionKind;
import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.api.TriageSource;
import com.coldchain.triage.internal.domain.model.AlertStatus;
import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.InboxEvent;
import com.coldchain.triage.internal.domain.model.InboxStatus;
import com.coldchain.triage.internal.domain.model.Summary;
import com.coldchain.triage.internal.domain.model.Triage;
import com.coldchain.triage.internal.domain.model.TriageProvenance;
import com.coldchain.triage.internal.infrastructure.persistence.entity.ExcursionAlertJpaEntity;
import com.coldchain.triage.internal.infrastructure.persistence.entity.InboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TriagePersistenceMapper {

    public ExcursionAlertJpaEntity toEntity(ExcursionAlert alert) {
        ExcursionAlertJpaEntity entity = new ExcursionAlertJpaEntity();
        entity.setId(alert.id());
        entity.setExcursionId(alert.excursionId());
        entity.setShipmentId(alert.shipmentId());
        entity.setOrganizationId(alert.organizationId());
        entity.setKind(alert.kind().name());
        entity.setStatus(alert.status().name());
        entity.setOpenedAt(alert.openedAt());
        entity.setClosedAt(alert.closedAt());
        entity.setPeakCelsius(alert.peakCelsius());
        entity.setDurationMinutes(alert.durationMinutes() == null ? null : Math.toIntExact(alert.durationMinutes()));
        entity.setVersion(alert.version());
        Triage triage = alert.triage();
        if (triage != null) {
            entity.setProbableCause(triage.probableCause().name());
            entity.setSeverity(triage.severity().name());
            entity.setConfidence(triage.confidence());
            entity.setSummaryEn(triage.summary().english());
            entity.setSummaryEs(triage.summary().spanish());
            entity.setSummaryPt(triage.summary().portuguese());
            entity.setRecommendedAction(triage.recommendedAction());
            entity.setSource(triage.source().name());
            entity.setModel(triage.provenance().model());
            entity.setPromptVersion(triage.provenance().promptVersion());
            entity.setInputTokens(triage.provenance().inputTokens());
            entity.setOutputTokens(triage.provenance().outputTokens());
            entity.setLatencyMs(triage.provenance().latencyMs());
            entity.setContextAvailable(triage.provenance().contextAvailable());
            entity.setTriagedAt(triage.triagedAt());
        }
        return entity;
    }

    public ExcursionAlert toDomain(ExcursionAlertJpaEntity entity) {
        return ExcursionAlert.restore(entity.getId(), entity.getExcursionId(), entity.getShipmentId(),
                entity.getOrganizationId(), ExcursionKind.valueOf(entity.getKind()),
                AlertStatus.valueOf(entity.getStatus()), entity.getOpenedAt(), entity.getClosedAt(),
                entity.getPeakCelsius(),
                entity.getDurationMinutes() == null ? null : entity.getDurationMinutes().longValue(),
                triageOf(entity), entity.getVersion());
    }

    public InboxEventJpaEntity toEntity(InboxEvent event) {
        InboxEventJpaEntity entity = new InboxEventJpaEntity();
        entity.setEventId(event.eventId());
        entity.setType(event.type().wireName());
        entity.setExcursionId(event.excursionId());
        entity.setOccurredAt(event.occurredAt());
        entity.setReceivedAt(event.receivedAt());
        entity.setStatus(event.status().name());
        entity.setProcessedAt(event.processedAt());
        return entity;
    }

    public InboxEvent toDomain(InboxEventJpaEntity entity) {
        ExcursionEventType type = ExcursionEventType.ofWireName(entity.getType()).orElseThrow(
                () -> new IllegalStateException("The inbox holds an event type nobody sends: " + entity.getType()));
        return new InboxEvent(entity.getEventId(), type, entity.getExcursionId(), entity.getOccurredAt(),
                entity.getReceivedAt(), InboxStatus.valueOf(entity.getStatus()), entity.getProcessedAt());
    }

    private static Triage triageOf(ExcursionAlertJpaEntity entity) {
        if (entity.getTriagedAt() == null) {
            return null;
        }
        return new Triage(ProbableCause.valueOf(entity.getProbableCause()), Severity.valueOf(entity.getSeverity()),
                entity.getConfidence(),
                new Summary(entity.getSummaryEn(), entity.getSummaryEs(), entity.getSummaryPt()),
                entity.getRecommendedAction(), TriageSource.valueOf(entity.getSource()),
                new TriageProvenance(entity.getModel(), entity.getPromptVersion(), entity.getInputTokens(),
                        entity.getOutputTokens(), entity.getLatencyMs(),
                        Boolean.TRUE.equals(entity.getContextAvailable())),
                entity.getTriagedAt());
    }
}
