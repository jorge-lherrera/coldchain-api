package com.coldchain.modules.telemetry.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import com.coldchain.modules.telemetry.internal.domain.repository.DeviceAssignmentRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa.DeviceAssignmentJpaRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.mapper.TelemetryPersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceAssignmentRepositoryAdapter implements DeviceAssignmentRepository {

    private final DeviceAssignmentJpaRepository assignments;

    private final TelemetryPersistenceMapper mapper;

    public DeviceAssignmentRepositoryAdapter(DeviceAssignmentJpaRepository assignments,
            TelemetryPersistenceMapper mapper) {
        this.assignments = assignments;
        this.mapper = mapper;
    }

    @Override
    public DeviceAssignment save(DeviceAssignment assignment) {
        try {
            assignments.saveAndFlush(mapper.toEntity(assignment));
        } catch (DataIntegrityViolationException cause) {
            throw TelemetryConstraintTranslation.translate(cause);
        }
        return assignment;
    }

    @Override
    public List<DeviceAssignment> findWindowsOf(UUID deviceId) {
        return assignments.findByDeviceIdOrderByAttachedAtAsc(deviceId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<DeviceAssignment> findOpen(UUID deviceId) {
        return assignments.findByDeviceIdAndDetachedAtIsNull(deviceId).map(mapper::toDomain);
    }
}
