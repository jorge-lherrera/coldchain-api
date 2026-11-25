package com.coldchain.modules.telemetry.internal.domain.repository;

import com.coldchain.modules.telemetry.internal.domain.model.Excursion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExcursionRepository {

    Excursion save(Excursion excursion);

    List<Excursion> findOfShipment(UUID shipmentId);

    Optional<Excursion> findById(UUID excursionId);

    void deleteAll(List<Excursion> excursions);
}
