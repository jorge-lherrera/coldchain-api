package com.coldchain.modules.shipment.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.shipment.api.HandoffStatus;
import com.coldchain.modules.shipment.internal.domain.model.HandoffRequest;
import com.coldchain.modules.shipment.internal.domain.repository.HandoffRequestRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa.HandoffRequestJpaRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.mapper.HandoffRequestPersistenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class HandoffRequestRepositoryAdapter implements HandoffRequestRepository {

    private final HandoffRequestJpaRepository requests;

    private final HandoffRequestPersistenceMapper mapper;

    public HandoffRequestRepositoryAdapter(HandoffRequestJpaRepository requests,
            HandoffRequestPersistenceMapper mapper) {
        this.requests = requests;
        this.mapper = mapper;
    }

    @Override
    public HandoffRequest save(HandoffRequest request) {
        try {
            requests.saveAndFlush(mapper.toEntity(request));
        } catch (DataIntegrityViolationException cause) {
            throw ShipmentConstraintTranslation.translate(cause);
        }
        return request;
    }

    @Override
    public Optional<HandoffRequest> findPending(UUID shipmentId) {
        return requests.findByShipmentIdAndStatus(shipmentId, HandoffStatus.PENDING.name())
                .map(mapper::toDomain);
    }
}
