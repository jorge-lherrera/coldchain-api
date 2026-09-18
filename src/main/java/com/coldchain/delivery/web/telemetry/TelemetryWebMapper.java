package com.coldchain.delivery.web.telemetry;

import com.coldchain.delivery.web.telemetry.dto.AssignDeviceRequest;
import com.coldchain.delivery.web.telemetry.dto.AssignmentResponse;
import com.coldchain.delivery.web.telemetry.dto.DeviceResponse;
import com.coldchain.delivery.web.telemetry.dto.IngestBatchRequest;
import com.coldchain.delivery.web.telemetry.dto.IngestBatchResponse;
import com.coldchain.delivery.web.telemetry.dto.RegisterDeviceRequest;
import com.coldchain.modules.telemetry.api.dto.AssignDeviceCommand;
import com.coldchain.modules.telemetry.api.dto.AssignmentResult;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.IngestBatchResult;
import com.coldchain.modules.telemetry.api.dto.MonitoringThresholds;
import com.coldchain.modules.telemetry.api.dto.ReadingCommand;
import com.coldchain.modules.telemetry.api.dto.RegisterDeviceCommand;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelemetryWebMapper {

    public RegisterDeviceCommand toCommand(UUID organizationId, RegisterDeviceRequest request) {
        return new RegisterDeviceCommand(organizationId, request.serialNumber(), request.model(),
                request.firmware(), request.samplingIntervalSeconds(), request.calibratedAt());
    }

    public AssignDeviceCommand toCommand(UUID deviceId, AssignDeviceRequest request) {
        return new AssignDeviceCommand(deviceId, request.shipmentId(),
                new MonitoringThresholds(request.minCelsius(), request.maxCelsius()),
                request.attachedAt());
    }

    public IngestBatchCommand toCommand(UUID organizationId, IngestBatchRequest request) {
        return new IngestBatchCommand(organizationId, request.deviceId(), request.idempotencyKey(),
                request.readings().stream()
                        .map(reading -> new ReadingCommand(reading.measuredAt(), reading.celsius()))
                        .toList());
    }

    public DeviceResponse toResponse(DeviceResult result) {
        return new DeviceResponse(result.id(), result.serialNumber(), result.model(),
                result.firmware(), result.status(), result.samplingIntervalSeconds(),
                result.calibratedAt());
    }

    public AssignmentResponse toResponse(AssignmentResult result) {
        return new AssignmentResponse(result.id(), result.deviceId(), result.shipmentId(),
                result.attachedAt(), result.detachedAt());
    }

    public IngestBatchResponse toResponse(IngestBatchResult result) {
        return new IngestBatchResponse(result.batchId(), result.status(), result.receivedCount(),
                result.acceptedCount(), result.discardedCount(), result.discarded());
    }
}
