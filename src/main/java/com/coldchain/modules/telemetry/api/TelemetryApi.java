package com.coldchain.modules.telemetry.api;

import com.coldchain.modules.telemetry.api.dto.AssignDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.AssignmentResult;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.IngestBatchResult;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.SeriesResult;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.time.Instant;
import java.util.UUID;

public interface TelemetryApi {

    DeviceResult registerDevice(RegisterDeviceCommand command);

    AssignmentResult assignDevice(AssignDeviceCommand command);

    AssignmentResult detachDevice(UUID deviceId, Instant detachedAt);

    DeviceResult deviceOf(UUID deviceId);

    IngestBatchResult ingestBatch(IngestBatchCommand command);

    PagedResult<DeviceResult> listDevices(UUID organizationId, PageCriteria criteria);

    SeriesResult seriesOf(UUID shipmentId, UUID organizationId);
}
