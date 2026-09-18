package com.coldchain.modules.compliance.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.compliance.internal.infrastructure.persistence.entity.CertificateJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateJpaRepository
        extends JpaRepository<CertificateJpaEntity, UUID> {

    Optional<CertificateJpaEntity> findByShipmentIdAndSupersededAtIsNull(UUID shipmentId);

    Optional<CertificateJpaEntity> findFirstByShipmentIdOrderByCertificateVersionDesc(
            UUID shipmentId);
}
