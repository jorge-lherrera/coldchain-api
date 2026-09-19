package com.coldchain.modules.telemetry.internal.application.usecase.command;

import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import com.coldchain.modules.telemetry.internal.application.mapper.TelemetryApiMapper;
import com.coldchain.modules.telemetry.internal.domain.model.Device;
import com.coldchain.modules.telemetry.internal.domain.repository.DeviceRepository;
import com.coldchain.shared.annotation.UseCase;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RegisterDeviceUseCase {

    private final DeviceRepository devices;

    private final TelemetryApiMapper mapper;

    public RegisterDeviceUseCase(DeviceRepository devices, TelemetryApiMapper mapper) {
        this.devices = devices;
        this.mapper = mapper;
    }

    @Transactional
    public DeviceResult execute(RegisterDeviceCommand command) {
        return mapper.toResult(devices.save(Device.register(command.organizationId(),
                command.serialNumber(), command.model(), command.firmware(),
                command.samplingIntervalSeconds(), command.calibratedAt())));
    }
}
