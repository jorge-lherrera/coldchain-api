package com.coldchain.modules.telemetry.internal.application.usecase.command;

import com.coldchain.modules.telemetry.api.dto.AssignmentResult;
import com.coldchain.modules.telemetry.internal.application.mapper.TelemetryApiMapper;
import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import com.coldchain.modules.telemetry.internal.domain.repository.DeviceAssignmentRepository;
import com.coldchain.modules.telemetry.internal.exception.TelemetryErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class DetachDeviceUseCase {

    private final DeviceAssignmentRepository assignments;

    private final TelemetryApiMapper mapper;

    public DetachDeviceUseCase(DeviceAssignmentRepository assignments, TelemetryApiMapper mapper) {
        this.assignments = assignments;
        this.mapper = mapper;
    }

    @Transactional
    public AssignmentResult execute(UUID deviceId, Instant detachedAt) {
        DeviceAssignment open = assignments.findOpen(deviceId)
                .orElseThrow(() -> DomainException.of(TelemetryErrorCode.DEVICE_NOT_ASSIGNED));
        return mapper.toResult(assignments.save(open.detach(detachedAt)));
    }
}
