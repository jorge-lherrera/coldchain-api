package com.coldchain.modules.telemetry.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.telemetry.api.BatchStatus;
import com.coldchain.modules.telemetry.api.DeviceStatus;
import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.api.ExcursionStatus;
import com.coldchain.modules.telemetry.internal.domain.model.Device;
import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import com.coldchain.modules.telemetry.internal.domain.model.Excursion;
import com.coldchain.modules.telemetry.internal.domain.model.ReadingBatch;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.DeviceAssignmentJpaEntity;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.DeviceJpaEntity;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.ExcursionJpaEntity;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.ReadingBatchJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TelemetryPersistenceMapper {

    public DeviceJpaEntity toEntity(Device device) {
        return new DeviceJpaEntity(device.id(), device.organizationId(), device.serialNumber(),
                device.model(), device.firmware(), device.status().name(),
                device.samplingIntervalSeconds(), device.calibratedAt(), device.deletedAt());
    }

    public Device toDomain(DeviceJpaEntity entity) {
        return Device.restore(entity.getId(), entity.getOrganizationId(),
                entity.getSerialNumber(), entity.getModel(), entity.getFirmware(),
                DeviceStatus.valueOf(entity.getStatus()), entity.getSamplingIntervalSeconds(),
                entity.getCalibratedAt(), entity.getDeletedAt());
    }

    public DeviceAssignmentJpaEntity toEntity(DeviceAssignment assignment) {
        return new DeviceAssignmentJpaEntity(assignment.id(), assignment.deviceId(),
                assignment.shipmentId(), assignment.minCelsius(), assignment.maxCelsius(),
                assignment.attachedAt(), assignment.detachedAt());
    }

    public DeviceAssignment toDomain(DeviceAssignmentJpaEntity entity) {
        return DeviceAssignment.restore(entity.getId(), entity.getDeviceId(), entity.getShipmentId(),
                entity.getMinCelsius(), entity.getMaxCelsius(), entity.getAttachedAt(),
                entity.getDetachedAt());
    }

    public ReadingBatchJpaEntity toEntity(ReadingBatch batch) {
        return new ReadingBatchJpaEntity(batch.id(), batch.organizationId(), batch.deviceId(),
                batch.idempotencyKey(), batch.status().name(), batch.receivedCount(),
                batch.acceptedCount(), batch.discardedCount(), batch.receivedAt());
    }

    public ReadingBatch toDomain(ReadingBatchJpaEntity entity) {
        return ReadingBatch.restore(entity.getId(), entity.getOrganizationId(), entity.getDeviceId(),
                entity.getIdempotencyKey(), BatchStatus.valueOf(entity.getStatus()),
                entity.getReceivedCount(), entity.getAcceptedCount(), entity.getDiscardedCount(),
                entity.getReceivedAt());
    }

    public ExcursionJpaEntity toEntity(Excursion excursion) {
        return new ExcursionJpaEntity(excursion.id(), excursion.organizationId(),
                excursion.shipmentId(), excursion.kind().name(), excursion.status().name(),
                excursion.openedAt(), excursion.closedAt(), excursion.openedByReadingId(),
                excursion.closedByReadingId(), excursion.peakCelsius(), excursion.durationMinutes());
    }

    public Excursion toDomain(ExcursionJpaEntity entity) {
        return Excursion.restore(entity.getId(), entity.getOrganizationId(), entity.getShipmentId(),
                ExcursionKind.valueOf(entity.getKind()), ExcursionStatus.valueOf(entity.getStatus()),
                entity.getOpenedAt(), entity.getClosedAt(), entity.getOpenedByReadingId(),
                entity.getClosedByReadingId(), entity.getPeakCelsius(), entity.getDurationMinutes());
    }
}
