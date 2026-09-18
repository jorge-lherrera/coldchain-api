package com.coldchain.modules.telemetry.internal.application.usecase.command;

import com.coldchain.modules.telemetry.api.BatchStatus;
import com.coldchain.modules.telemetry.api.DiscardReason;
import com.coldchain.modules.telemetry.api.dto.DiscardedReading;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.IngestBatchResult;
import com.coldchain.modules.telemetry.api.dto.ReadingCommand;
import com.coldchain.modules.telemetry.internal.application.ExcursionReview;
import com.coldchain.modules.telemetry.internal.domain.model.DeviceAssignment;
import com.coldchain.modules.telemetry.internal.domain.model.ReadingBatch;
import com.coldchain.modules.telemetry.internal.domain.model.SensorDevice;
import com.coldchain.modules.telemetry.internal.domain.model.TemperatureReading;
import com.coldchain.modules.telemetry.internal.domain.repository.DeviceAssignmentRepository;
import com.coldchain.modules.telemetry.internal.domain.repository.ReadingBatchRepository;
import com.coldchain.modules.telemetry.internal.domain.repository.SensorDeviceRepository;
import com.coldchain.modules.telemetry.internal.domain.repository.TemperatureReadingRepository;
import com.coldchain.modules.telemetry.internal.domain.service.AssignmentWindowResolver;
import com.coldchain.modules.telemetry.internal.exception.TelemetryErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class IngestBatchUseCase {

    private static final BigDecimal COLDEST_POSSIBLE = new BigDecimal("-100");

    private static final BigDecimal HOTTEST_POSSIBLE = new BigDecimal("100");

    private final SensorDeviceRepository devices;

    private final DeviceAssignmentRepository assignments;

    private final ReadingBatchRepository batches;

    private final TemperatureReadingRepository readings;

    private final ExcursionReview excursionReview;

    private final Clock clock;

    public IngestBatchUseCase(SensorDeviceRepository devices, DeviceAssignmentRepository assignments,
            ReadingBatchRepository batches, TemperatureReadingRepository readings,
            ExcursionReview excursionReview, Clock clock) {
        this.devices = devices;
        this.assignments = assignments;
        this.batches = batches;
        this.readings = readings;
        this.excursionReview = excursionReview;
        this.clock = clock;
    }

    @Transactional
    public IngestBatchResult execute(IngestBatchCommand command) {
        Optional<ReadingBatch> replayed = batches.findByIdempotencyKey(command.idempotencyKey());
        if (replayed.isPresent()) {
            ReadingBatch original = replayed.get();
            return new IngestBatchResult(original.id(), BatchStatus.REPLAYED, original.receivedCount(),
                    original.acceptedCount(), original.discardedCount(), List.of());
        }
        if (command.readings().isEmpty()) {
            throw DomainException.of(TelemetryErrorCode.EMPTY_BATCH);
        }
        SensorDevice device = devices.findById(command.deviceId())
                .orElseThrow(() -> DomainException.of(TelemetryErrorCode.DEVICE_NOT_FOUND));
        if (!device.usable()) {
            throw DomainException.of(TelemetryErrorCode.DEVICE_NOT_USABLE);
        }

        List<DeviceAssignment> windows = assignments.findWindowsOf(device.id());
        Set<Instant> alreadyStored = new HashSet<>(readings.findMeasuredAtOf(device.id(),
                earliest(command.readings()), latest(command.readings())));
        Set<Instant> seenInThisBatch = new LinkedHashSet<>();
        List<TemperatureReading> accepted = new ArrayList<>();
        List<DiscardedReading> discarded = new ArrayList<>();
        UUID batchId = com.coldchain.shared.identifier.UuidV7.generate();

        for (ReadingCommand reading : command.readings()) {
            Instant measuredAt = reading.measuredAt().truncatedTo(ChronoUnit.MICROS);
            if (reading.celsius().compareTo(COLDEST_POSSIBLE) < 0
                    || reading.celsius().compareTo(HOTTEST_POSSIBLE) > 0) {
                discarded.add(new DiscardedReading(measuredAt, DiscardReason.IMPOSSIBLE_VALUE));
                continue;
            }
            if (alreadyStored.contains(measuredAt) || !seenInThisBatch.add(measuredAt)) {
                discarded.add(new DiscardedReading(measuredAt, DiscardReason.DUPLICATE));
                continue;
            }
            Optional<DeviceAssignment> window = AssignmentWindowResolver.resolve(windows, measuredAt);
            if (window.isEmpty()) {
                discarded.add(new DiscardedReading(measuredAt, DiscardReason.OUT_OF_WINDOW));
                continue;
            }
            accepted.add(TemperatureReading.createNew(command.organizationId(), device.id(),
                    window.get().shipmentId(), batchId, reading.celsius(), measuredAt));
        }

        readings.saveAll(accepted);
        ReadingBatch batch = batches.save(ReadingBatch.record(command.organizationId(), device.id(),
                command.idempotencyKey(), command.readings().size(), accepted.size(),
                discarded.size(), clock.instant()));
        excursionReview.reviewShipmentsTouchedBy(accepted, windows);
        return new IngestBatchResult(batch.id(), batch.status(), batch.receivedCount(),
                batch.acceptedCount(), batch.discardedCount(), List.copyOf(discarded));
    }

    private static Instant earliest(List<ReadingCommand> readings) {
        return readings.stream().map(ReadingCommand::measuredAt).min(Instant::compareTo).orElseThrow();
    }

    private static Instant latest(List<ReadingCommand> readings) {
        return readings.stream().map(ReadingCommand::measuredAt).max(Instant::compareTo).orElseThrow();
    }
}
