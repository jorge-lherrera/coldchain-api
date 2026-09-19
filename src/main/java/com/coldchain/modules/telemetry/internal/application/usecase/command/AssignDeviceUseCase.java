package com.coldchain.modules.telemetry.internal.application.usecase.command;

import com.coldchain.modules.telemetry.api.dto.AssignDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.AssignmentResult;
import com.coldchain.modules.telemetry.internal.application.mapper.TelemetryApiMapper;
import com.coldchain.modules.telemetry.internal.domain.model.Device;
import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import com.coldchain.modules.telemetry.internal.domain.repository.DeviceAssignmentRepository;
import com.coldchain.modules.telemetry.internal.domain.repository.DeviceRepository;
import com.coldchain.modules.telemetry.internal.exception.TelemetryErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class AssignDeviceUseCase {

    private final DeviceRepository devices;

    private final DeviceAssignmentRepository assignments;

    private final TelemetryApiMapper mapper;

    public AssignDeviceUseCase(DeviceRepository devices, DeviceAssignmentRepository assignments,
            TelemetryApiMapper mapper) {
        this.devices = devices;
        this.assignments = assignments;
        this.mapper = mapper;
    }

    @Transactional
    public AssignmentResult execute(AssignDeviceCommand command) {
        Device device = devices.findById(command.deviceId())
                .orElseThrow(() -> DomainException.of(TelemetryErrorCode.DEVICE_NOT_FOUND));
        if (!device.usable()) {
            throw DomainException.of(TelemetryErrorCode.DEVICE_NOT_USABLE);
        }
        return mapper.toResult(assignments.save(DeviceAssignment.attach(device.id(),
                command.shipmentId(), command.thresholds().minCelsius(),
                command.thresholds().maxCelsius(), command.attachedAt())));
    }
}
