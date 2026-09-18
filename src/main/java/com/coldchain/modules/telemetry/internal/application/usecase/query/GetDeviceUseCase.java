package com.coldchain.modules.telemetry.internal.application.usecase.query;

import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.internal.application.mapper.TelemetryApiMapper;
import com.coldchain.modules.telemetry.internal.domain.repository.SensorDeviceRepository;
import com.coldchain.modules.telemetry.internal.exception.TelemetryErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.util.UUID;

@UseCase
public class GetDeviceUseCase {

    private final SensorDeviceRepository devices;

    private final TelemetryApiMapper mapper;

    public GetDeviceUseCase(SensorDeviceRepository devices, TelemetryApiMapper mapper) {
        this.devices = devices;
        this.mapper = mapper;
    }

    public DeviceResult execute(UUID deviceId) {
        return devices.findById(deviceId).map(mapper::toResult)
                .orElseThrow(() -> DomainException.of(TelemetryErrorCode.DEVICE_NOT_FOUND));
    }
}
