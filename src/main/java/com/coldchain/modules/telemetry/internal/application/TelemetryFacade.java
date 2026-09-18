package com.coldchain.modules.telemetry.internal.application;

import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.AssignDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.AssignmentResult;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.IngestBatchResult;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.SeriesResult;
import com.coldchain.modules.telemetry.internal.application.usecase.command.AssignDeviceUseCase;
import com.coldchain.modules.telemetry.internal.application.usecase.command.DetachDeviceUseCase;
import com.coldchain.modules.telemetry.internal.application.usecase.command.IngestBatchUseCase;
import com.coldchain.modules.telemetry.internal.application.usecase.command.RegisterDeviceUseCase;
import com.coldchain.modules.telemetry.internal.application.usecase.query.GetDeviceUseCase;
import com.coldchain.modules.telemetry.internal.application.usecase.query.ListDevicesUseCase;
import com.coldchain.modules.telemetry.internal.application.usecase.query.QuerySeriesUseCase;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelemetryFacade implements TelemetryApi {

    private final RegisterDeviceUseCase registerDevice;

    private final AssignDeviceUseCase assignDevice;

    private final DetachDeviceUseCase detachDevice;

    private final IngestBatchUseCase ingestBatch;

    private final ListDevicesUseCase listDevices;

    private final GetDeviceUseCase getDevice;

    private final QuerySeriesUseCase querySeries;

    public TelemetryFacade(RegisterDeviceUseCase registerDevice, AssignDeviceUseCase assignDevice,
            DetachDeviceUseCase detachDevice, IngestBatchUseCase ingestBatch,
            ListDevicesUseCase listDevices, GetDeviceUseCase getDevice,
            QuerySeriesUseCase querySeries) {
        this.registerDevice = registerDevice;
        this.assignDevice = assignDevice;
        this.detachDevice = detachDevice;
        this.ingestBatch = ingestBatch;
        this.listDevices = listDevices;
        this.getDevice = getDevice;
        this.querySeries = querySeries;
    }

    @Override
    public DeviceResult registerDevice(RegisterDeviceCommand command) {
        return registerDevice.execute(command);
    }

    @Override
    public AssignmentResult assignDevice(AssignDeviceCommand command) {
        return assignDevice.execute(command);
    }

    @Override
    public AssignmentResult detachDevice(UUID deviceId, Instant detachedAt) {
        return detachDevice.execute(deviceId, detachedAt);
    }

    @Override
    public DeviceResult deviceOf(UUID deviceId) {
        return getDevice.execute(deviceId);
    }

    @Override
    public IngestBatchResult ingestBatch(IngestBatchCommand command) {
        return ingestBatch.execute(command);
    }

    @Override
    public PagedResult<DeviceResult> listDevices(UUID organizationId, PageCriteria criteria) {
        return listDevices.execute(organizationId, criteria);
    }

    @Override
    public SeriesResult seriesOf(UUID shipmentId, UUID organizationId) {
        return querySeries.execute(shipmentId, organizationId);
    }
}
