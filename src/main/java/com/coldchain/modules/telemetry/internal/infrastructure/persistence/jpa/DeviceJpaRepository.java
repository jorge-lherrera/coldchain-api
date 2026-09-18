package com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.DeviceJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceJpaRepository extends JpaRepository<DeviceJpaEntity, UUID> {

    Page<DeviceJpaEntity> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId,
            Pageable pageable);
}
