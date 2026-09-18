package com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.DeviceAssignmentJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceAssignmentJpaRepository extends JpaRepository<DeviceAssignmentJpaEntity, UUID> {

    List<DeviceAssignmentJpaEntity> findByDeviceIdOrderByAttachedAtAsc(UUID deviceId);

    Optional<DeviceAssignmentJpaEntity> findByDeviceIdAndDetachedAtIsNull(UUID deviceId);
}
