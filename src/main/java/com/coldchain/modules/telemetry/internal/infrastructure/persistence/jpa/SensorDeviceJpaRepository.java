package com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.SensorDeviceJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorDeviceJpaRepository extends JpaRepository<SensorDeviceJpaEntity, UUID> {

    Page<SensorDeviceJpaEntity> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId,
            Pageable pageable);
}
