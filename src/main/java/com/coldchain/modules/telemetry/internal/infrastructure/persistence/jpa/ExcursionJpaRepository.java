package com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.ExcursionJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcursionJpaRepository extends JpaRepository<ExcursionJpaEntity, UUID> {

    List<ExcursionJpaEntity> findByShipmentIdOrderByOpenedAtAsc(UUID shipmentId);

    int deleteByShipmentId(UUID shipmentId);
}
