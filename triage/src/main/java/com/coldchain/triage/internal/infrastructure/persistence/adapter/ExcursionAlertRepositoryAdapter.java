package com.coldchain.triage.internal.infrastructure.persistence.adapter;

import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.repository.ExcursionAlertRepository;
import com.coldchain.triage.internal.infrastructure.persistence.jpa.ExcursionAlertJpaRepository;
import com.coldchain.triage.internal.infrastructure.persistence.mapper.TriagePersistenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ExcursionAlertRepositoryAdapter implements ExcursionAlertRepository {

    private final ExcursionAlertJpaRepository alerts;

    private final TriagePersistenceMapper mapper;

    public ExcursionAlertRepositoryAdapter(ExcursionAlertJpaRepository alerts, TriagePersistenceMapper mapper) {
        this.alerts = alerts;
        this.mapper = mapper;
    }

    @Override
    public Optional<ExcursionAlert> findByExcursionId(UUID excursionId) {
        return alerts.findByExcursionId(excursionId).map(mapper::toDomain);
    }

    @Override
    public ExcursionAlert save(ExcursionAlert alert) {
        alerts.saveAndFlush(mapper.toEntity(alert));
        return alert;
    }
}
