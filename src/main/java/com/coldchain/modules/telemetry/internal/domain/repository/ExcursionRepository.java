package com.coldchain.modules.telemetry.internal.domain.repository;

import com.coldchain.modules.telemetry.internal.domain.model.Excursion;
import java.util.List;
import java.util.UUID;

public interface ExcursionRepository {

    Excursion save(Excursion excursion);

    List<Excursion> findOfShipment(UUID shipmentId);

    int deleteOfShipment(UUID shipmentId);
}
