package com.coldchain.modules.shipment.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentParticipant;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.ShipmentParticipantJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ShipmentParticipantPersistenceMapper {

    public ShipmentParticipantJpaEntity toEntity(ShipmentParticipant participant) {
        return new ShipmentParticipantJpaEntity(participant.id(), participant.shipmentId(),
                participant.organizationId(), participant.participation().name(),
                participant.revokedAt());
    }

    public ShipmentParticipant toDomain(ShipmentParticipantJpaEntity entity) {
        return ShipmentParticipant.restore(entity.getId(), entity.getShipmentId(),
                entity.getOrganizationId(), Participation.valueOf(entity.getParticipation()),
                entity.getRevokedAt());
    }
}
