package com.coldchain.modules.telemetry.internal.application.usecase.query;

import com.coldchain.modules.telemetry.api.dto.ExcursionWindowResult;
import com.coldchain.modules.telemetry.internal.application.mapper.TelemetryApiMapper;
import com.coldchain.modules.telemetry.internal.domain.model.Excursion;
import com.coldchain.modules.telemetry.internal.domain.repository.ExcursionRepository;
import com.coldchain.modules.telemetry.internal.domain.repository.TemperatureReadingRepository;
import com.coldchain.modules.telemetry.internal.exception.TelemetryErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@UseCase
public class ExcursionWindowUseCase {

    private final ExcursionRepository excursions;

    private final TemperatureReadingRepository readings;

    private final TelemetryApiMapper mapper;

    public ExcursionWindowUseCase(ExcursionRepository excursions, TemperatureReadingRepository readings,
            TelemetryApiMapper mapper) {
        this.excursions = excursions;
        this.readings = readings;
        this.mapper = mapper;
    }

    public ExcursionWindowResult execute(UUID shipmentId, UUID excursionId, Duration margin) {
        Excursion excursion = excursions.findById(excursionId)
                .filter(found -> found.shipmentId().equals(shipmentId))
                .orElseThrow(() -> DomainException.of(TelemetryErrorCode.EXCURSION_NOT_FOUND));
        Instant from = excursion.openedAt().minus(margin);
        Instant to = excursion.lastSeenAt().plus(margin);
        return new ExcursionWindowResult(mapper.toResult(excursion), from, to,
                readings.findOfShipmentBetween(shipmentId, from, to).stream()
                        .map(mapper::toPoint)
                        .toList());
    }
}
