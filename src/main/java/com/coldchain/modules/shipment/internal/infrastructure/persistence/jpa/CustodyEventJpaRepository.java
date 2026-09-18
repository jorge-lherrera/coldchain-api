package com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.CustodyEventJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustodyEventJpaRepository extends JpaRepository<CustodyEventJpaEntity, UUID> {

    List<CustodyEventJpaEntity> findByShipmentIdOrderBySequenceNumberAsc(UUID shipmentId);

    Optional<CustodyEventJpaEntity> findFirstByShipmentIdOrderBySequenceNumberDesc(UUID shipmentId);
}
