package com.coldchain.modules.compliance.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.compliance.internal.infrastructure.persistence.entity.ComplianceCertificateJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceCertificateJpaRepository
        extends JpaRepository<ComplianceCertificateJpaEntity, UUID> {

    Optional<ComplianceCertificateJpaEntity> findByShipmentIdAndSupersededAtIsNull(UUID shipmentId);

    Optional<ComplianceCertificateJpaEntity> findFirstByShipmentIdOrderByCertificateVersionDesc(
            UUID shipmentId);
}
