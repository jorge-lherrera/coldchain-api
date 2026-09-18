package com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.HandoffRequestJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoffRequestJpaRepository extends JpaRepository<HandoffRequestJpaEntity, UUID> {

    Optional<HandoffRequestJpaEntity> findByShipmentIdAndStatus(UUID shipmentId, String status);
}
