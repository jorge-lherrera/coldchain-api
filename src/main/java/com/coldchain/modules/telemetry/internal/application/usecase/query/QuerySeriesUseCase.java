package com.coldchain.modules.telemetry.internal.application.usecase.query;

import com.coldchain.modules.telemetry.api.dto.SeriesResult;
import com.coldchain.modules.telemetry.internal.application.mapper.TelemetryApiMapper;
import com.coldchain.modules.telemetry.internal.domain.repository.ExcursionRepository;
import com.coldchain.modules.telemetry.internal.domain.repository.TemperatureReadingRepository;
import com.coldchain.shared.application.UseCase;
import java.util.UUID;

@UseCase
public class QuerySeriesUseCase {

    private final TemperatureReadingRepository readings;

    private final ExcursionRepository excursions;

    private final TelemetryApiMapper mapper;

    public QuerySeriesUseCase(TemperatureReadingRepository readings, ExcursionRepository excursions,
            TelemetryApiMapper mapper) {
        this.readings = readings;
        this.excursions = excursions;
        this.mapper = mapper;
    }

    public SeriesResult execute(UUID shipmentId, UUID organizationId) {
        return new SeriesResult(shipmentId,
                readings.findOfShipment(shipmentId).stream()
                        .filter(reading -> reading.organizationId().equals(organizationId))
                        .map(mapper::toPoint)
                        .toList(),
                excursions.findOfShipment(shipmentId).stream()
                        .filter(excursion -> excursion.organizationId().equals(organizationId))
                        .map(mapper::toResult)
                        .toList());
    }
}
