package com.coldchain.modules.advisory.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.advisory.api.AlertSeverity;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import com.coldchain.modules.advisory.internal.domain.model.Advice;
import com.coldchain.modules.advisory.internal.domain.model.ExcursionAlert;
import com.coldchain.modules.advisory.internal.infrastructure.persistence.entity.ExcursionAlertJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AdvisoryPersistenceMapper {

    public ExcursionAlertJpaEntity toEntity(ExcursionAlert alert) {
        Advice advice = alert.advice();
        return new ExcursionAlertJpaEntity(alert.id(), alert.organizationId(), alert.shipmentId(),
                alert.excursionId(), advice.probableCause().name(), advice.severity().name(), advice.confidence(),
                advice.summaryEn(), advice.summaryEs(), advice.summaryPt(), advice.recommendedAction(),
                advice.source().name(), advice.model(), advice.contextAvailable(), advice.triagedAt(),
                alert.lockVersion());
    }

    public ExcursionAlert toDomain(ExcursionAlertJpaEntity entity) {
        return ExcursionAlert.restore(entity.getId(), entity.getOrganizationId(), entity.getShipmentId(),
                entity.getExcursionId(), new Advice(ProbableCause.valueOf(entity.getProbableCause()),
                        AlertSeverity.valueOf(entity.getSeverity()), entity.getConfidence(), entity.getSummaryEn(),
                        entity.getSummaryEs(), entity.getSummaryPt(), entity.getRecommendedAction(),
                        AlertSource.valueOf(entity.getSource()), entity.getModel(), entity.getContextAvailable(),
                        entity.getTriagedAt()),
                entity.getLockVersion());
    }
}
