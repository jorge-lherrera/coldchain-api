package com.coldchain.modules.compliance.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.compliance.internal.domain.model.ComplianceCertificate;
import com.coldchain.modules.compliance.internal.domain.repository.CertificateRepository;
import com.coldchain.modules.compliance.internal.infrastructure.persistence.entity.ComplianceCertificateJpaEntity;
import com.coldchain.modules.compliance.internal.infrastructure.persistence.jpa.CertificateFindingJpaRepository;
import com.coldchain.modules.compliance.internal.infrastructure.persistence.jpa.ComplianceCertificateJpaRepository;
import com.coldchain.modules.compliance.internal.infrastructure.persistence.mapper.CertificatePersistenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CertificateRepositoryAdapter implements CertificateRepository {

    private final ComplianceCertificateJpaRepository certificates;

    private final CertificateFindingJpaRepository findings;

    private final CertificatePersistenceMapper mapper;

    public CertificateRepositoryAdapter(ComplianceCertificateJpaRepository certificates,
            CertificateFindingJpaRepository findings, CertificatePersistenceMapper mapper) {
        this.certificates = certificates;
        this.findings = findings;
        this.mapper = mapper;
    }

    @Override
    public ComplianceCertificate save(ComplianceCertificate certificate) {
        certificates.saveAndFlush(mapper.toEntity(certificate));
        certificate.findings().stream().map(mapper::toEntity).forEach(findings::save);
        findings.flush();
        return certificate;
    }

    @Override
    public Optional<ComplianceCertificate> findCurrent(UUID shipmentId) {
        return certificates.findByShipmentIdAndSupersededAtIsNull(shipmentId)
                .map(entity -> mapper.toDomain(entity, findings.findByCertificateId(entity.getId())));
    }

    @Override
    public int highestVersionOf(UUID shipmentId) {
        return certificates.findFirstByShipmentIdOrderByCertificateVersionDesc(shipmentId)
                .map(ComplianceCertificateJpaEntity::getCertificateVersion)
                .orElse(0);
    }
}
