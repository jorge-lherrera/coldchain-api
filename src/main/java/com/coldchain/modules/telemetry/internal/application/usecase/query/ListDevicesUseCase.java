package com.coldchain.modules.telemetry.internal.application.usecase.query;

import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.internal.application.mapper.TelemetryApiMapper;
import com.coldchain.modules.telemetry.internal.domain.repository.DeviceRepository;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;

@UseCase
public class ListDevicesUseCase {

    private final DeviceRepository devices;

    private final TelemetryApiMapper mapper;

    public ListDevicesUseCase(DeviceRepository devices, TelemetryApiMapper mapper) {
        this.devices = devices;
        this.mapper = mapper;
    }

    public PagedResult<DeviceResult> execute(UUID organizationId, PageCriteria criteria) {
        return devices.findByOrganization(organizationId, criteria).map(mapper::toResult);
    }
}
