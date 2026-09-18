package com.coldchain.modules.shipment.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.shipment.internal.domain.model.ShipmentParticipant;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentParticipantRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa.ShipmentParticipantJpaRepository;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.mapper.ShipmentParticipantPersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ShipmentParticipantRepositoryAdapter implements ShipmentParticipantRepository {

    private final ShipmentParticipantJpaRepository participants;

    private final ShipmentParticipantPersistenceMapper mapper;

    public ShipmentParticipantRepositoryAdapter(ShipmentParticipantJpaRepository participants,
            ShipmentParticipantPersistenceMapper mapper) {
        this.participants = participants;
        this.mapper = mapper;
    }

    @Override
    public ShipmentParticipant save(ShipmentParticipant participant) {
        try {
            participants.saveAndFlush(mapper.toEntity(participant));
        } catch (DataIntegrityViolationException cause) {
            throw ShipmentConstraintTranslation.translate(cause);
        }
        return participant;
    }

    @Override
    public List<ShipmentParticipant> findLiveOf(UUID shipmentId) {
        return participants.findByShipmentIdAndRevokedAtIsNull(shipmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ShipmentParticipant> findLive(UUID shipmentId, UUID organizationId) {
        return participants.findByShipmentIdAndOrganizationIdAndRevokedAtIsNull(shipmentId,
                organizationId).map(mapper::toDomain);
    }
}
