package com.coldchain.modules.telemetry.internal.domain.repository;

import com.coldchain.modules.telemetry.internal.domain.model.TemperatureReading;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TemperatureReadingRepository {

    int saveAll(List<TemperatureReading> readings);

    List<TemperatureReading> findOfShipment(UUID shipmentId);

    List<Instant> findMeasuredAtOf(UUID deviceId, Instant from, Instant to);
}
