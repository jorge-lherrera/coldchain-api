package com.coldchain.modules.telemetry.internal.application.mapper;

import com.coldchain.modules.telemetry.api.dto.AssignmentResult;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.ExcursionResult;
import com.coldchain.modules.telemetry.api.dto.SeriesPoint;
import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import com.coldchain.modules.telemetry.internal.domain.model.Excursion;
import com.coldchain.modules.telemetry.internal.domain.model.SensorDevice;
import com.coldchain.modules.telemetry.internal.domain.model.TemperatureReading;
import org.springframework.stereotype.Component;

@Component
public class TelemetryApiMapper {

    public DeviceResult toResult(SensorDevice device) {
        return new DeviceResult(device.id(), device.organizationId(), device.serialNumber(),
                device.model(), device.firmware(), device.status(), device.samplingIntervalSeconds(),
                device.calibratedAt());
    }

    public AssignmentResult toResult(DeviceAssignment assignment) {
        return new AssignmentResult(assignment.id(), assignment.deviceId(), assignment.shipmentId(),
                assignment.attachedAt(), assignment.detachedAt());
    }

    public SeriesPoint toPoint(TemperatureReading reading) {
        return new SeriesPoint(reading.measuredAt(), reading.celsius());
    }

    public ExcursionResult toResult(Excursion excursion) {
        return new ExcursionResult(excursion.id(), excursion.shipmentId(), excursion.kind(),
                excursion.status(), excursion.openedAt(), excursion.closedAt(),
                excursion.peakCelsius(), excursion.durationMinutes());
    }
}
