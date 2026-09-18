package com.coldchain.modules.telemetry.internal.application.usecase.command;

import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import com.coldchain.modules.telemetry.internal.application.mapper.TelemetryApiMapper;
import com.coldchain.modules.telemetry.internal.domain.model.SensorDevice;
import com.coldchain.modules.telemetry.internal.domain.repository.SensorDeviceRepository;
import com.coldchain.shared.application.UseCase;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RegisterDeviceUseCase {

    private final SensorDeviceRepository devices;

    private final TelemetryApiMapper mapper;

    public RegisterDeviceUseCase(SensorDeviceRepository devices, TelemetryApiMapper mapper) {
        this.devices = devices;
        this.mapper = mapper;
    }

    @Transactional
    public DeviceResult execute(RegisterDeviceCommand command) {
        return mapper.toResult(devices.save(SensorDevice.register(command.organizationId(),
                command.serialNumber(), command.model(), command.firmware(),
                command.samplingIntervalSeconds(), command.calibratedAt())));
    }
}
