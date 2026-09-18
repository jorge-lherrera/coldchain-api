package com.coldchain.modules.shipment.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.ShipmentJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentJpaRepository extends JpaRepository<ShipmentJpaEntity, UUID> {

    @Query("""
            SELECT s FROM ShipmentJpaEntity s
            WHERE s.id = :shipmentId
              AND EXISTS (SELECT 1 FROM ShipmentParticipantJpaEntity p
                          WHERE p.shipmentId = s.id AND p.organizationId = :viewerOrganizationId
                            AND p.revokedAt IS NULL)
            """)
    Optional<ShipmentJpaEntity> findVisible(@Param("shipmentId") UUID shipmentId,
            @Param("viewerOrganizationId") UUID viewerOrganizationId);

    @Query("""
            SELECT s FROM ShipmentJpaEntity s
            WHERE EXISTS (SELECT 1 FROM ShipmentParticipantJpaEntity p
                          WHERE p.shipmentId = s.id AND p.organizationId = :viewerOrganizationId
                            AND p.revokedAt IS NULL)
            """)
    Page<ShipmentJpaEntity> findVisibleTo(@Param("viewerOrganizationId") UUID viewerOrganizationId,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ShipmentJpaEntity s SET s.hasOpenExcursion = :flag WHERE s.id = :shipmentId")
    int markOpenExcursion(@Param("shipmentId") UUID shipmentId, @Param("flag") Integer flag);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShipmentJpaEntity s WHERE s.id = :shipmentId")
    Optional<ShipmentJpaEntity> lock(@Param("shipmentId") UUID shipmentId);
}
