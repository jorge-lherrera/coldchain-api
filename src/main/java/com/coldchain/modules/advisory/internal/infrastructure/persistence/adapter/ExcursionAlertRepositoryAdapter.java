package com.coldchain.modules.advisory.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.advisory.internal.domain.model.ExcursionAlert;
import com.coldchain.modules.advisory.internal.domain.repository.ExcursionAlertRepository;
import com.coldchain.modules.advisory.internal.infrastructure.persistence.jpa.ExcursionAlertJpaRepository;
import com.coldchain.modules.advisory.internal.infrastructure.persistence.mapper.AdvisoryPersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ExcursionAlertRepositoryAdapter implements ExcursionAlertRepository {

    private final ExcursionAlertJpaRepository alerts;

    private final AdvisoryPersistenceMapper mapper;

    public ExcursionAlertRepositoryAdapter(ExcursionAlertJpaRepository alerts, AdvisoryPersistenceMapper mapper) {
        this.alerts = alerts;
        this.mapper = mapper;
    }

    @Override
    public ExcursionAlert save(ExcursionAlert alert) {
        return mapper.toDomain(alerts.saveAndFlush(mapper.toEntity(alert)));
    }

    @Override
    public Optional<ExcursionAlert> findByExcursionId(UUID excursionId) {
        return alerts.findByExcursionId(excursionId).map(mapper::toDomain);
    }

    @Override
    public List<ExcursionAlert> findOfShipment(UUID shipmentId) {
        return alerts.findByShipmentIdOrderByTriagedAtAsc(shipmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
