package com.coldchain.modules.telemetry.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.telemetry.internal.domain.model.Excursion;
import com.coldchain.modules.telemetry.internal.domain.repository.ExcursionRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa.ExcursionJpaRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.mapper.TelemetryPersistenceMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ExcursionRepositoryAdapter implements ExcursionRepository {

    private final ExcursionJpaRepository excursions;

    private final TelemetryPersistenceMapper mapper;

    public ExcursionRepositoryAdapter(ExcursionJpaRepository excursions,
            TelemetryPersistenceMapper mapper) {
        this.excursions = excursions;
        this.mapper = mapper;
    }

    @Override
    public Excursion save(Excursion excursion) {
        excursions.saveAndFlush(mapper.toEntity(excursion));
        return excursion;
    }

    @Override
    public List<Excursion> findOfShipment(UUID shipmentId) {
        return excursions.findByShipmentIdOrderByOpenedAtAsc(shipmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int deleteOfShipment(UUID shipmentId) {
        return excursions.deleteByShipmentId(shipmentId);
    }
}
