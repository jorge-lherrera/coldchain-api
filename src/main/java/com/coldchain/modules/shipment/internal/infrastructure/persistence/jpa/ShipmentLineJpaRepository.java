package com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.ShipmentLineJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentLineJpaRepository extends JpaRepository<ShipmentLineJpaEntity, UUID> {

    List<ShipmentLineJpaEntity> findByShipmentId(UUID shipmentId);
}
