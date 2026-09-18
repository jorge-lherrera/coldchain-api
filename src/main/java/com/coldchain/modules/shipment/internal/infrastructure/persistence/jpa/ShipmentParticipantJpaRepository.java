package com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.ShipmentParticipantJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentParticipantJpaRepository
        extends JpaRepository<ShipmentParticipantJpaEntity, UUID> {

    List<ShipmentParticipantJpaEntity> findByShipmentIdAndRevokedAtIsNull(UUID shipmentId);

    Optional<ShipmentParticipantJpaEntity> findByShipmentIdAndOrganizationIdAndRevokedAtIsNull(
            UUID shipmentId, UUID organizationId);
}
