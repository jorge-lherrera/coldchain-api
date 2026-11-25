package com.coldchain.modules.advisory.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.advisory.internal.infrastructure.persistence.entity.ExcursionAlertJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcursionAlertJpaRepository extends JpaRepository<ExcursionAlertJpaEntity, UUID> {

    Optional<ExcursionAlertJpaEntity> findByExcursionId(UUID excursionId);

    List<ExcursionAlertJpaEntity> findByShipmentIdOrderByTriagedAtAsc(UUID shipmentId);
}
