package com.coldchain.triage.internal.infrastructure.persistence.jpa;

import com.coldchain.triage.internal.infrastructure.persistence.entity.ExcursionAlertJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcursionAlertJpaRepository extends JpaRepository<ExcursionAlertJpaEntity, UUID> {

    Optional<ExcursionAlertJpaEntity> findByExcursionId(UUID excursionId);
}
