package com.coldchain.modules.shipment.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.shipment.internal.domain.model.CustodyEvent;
import com.coldchain.modules.shipment.internal.domain.repository.CustodyEventRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa.CustodyEventJpaRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa.ShipmentJpaRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.mapper.CustodyEventPersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CustodyEventRepositoryAdapter implements CustodyEventRepository {

    private final CustodyEventJpaRepository events;

    private final ShipmentJpaRepository shipments;

    private final CustodyEventPersistenceMapper mapper;

    public CustodyEventRepositoryAdapter(CustodyEventJpaRepository events,
            ShipmentJpaRepository shipments, CustodyEventPersistenceMapper mapper) {
        this.events = events;
        this.shipments = shipments;
        this.mapper = mapper;
    }

    @Override
    public CustodyEvent append(CustodyEvent event) {
        shipments.lock(event.shipmentId());
        events.saveAndFlush(mapper.toEntity(event));
        return event;
    }

    @Override
    public List<CustodyEvent> findOrderedBySequence(UUID shipmentId) {
        return events.findByShipmentIdOrderBySequenceNumberAsc(shipmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CustodyEvent> findLast(UUID shipmentId) {
        shipments.lock(shipmentId);
        return events.findFirstByShipmentIdOrderBySequenceNumberDesc(shipmentId).map(mapper::toDomain);
    }
}
