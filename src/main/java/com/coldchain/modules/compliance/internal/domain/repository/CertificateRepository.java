package com.coldchain.modules.compliance.internal.domain.repository;

import com.coldchain.modules.compliance.internal.domain.model.ComplianceCertificate;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository {

    ComplianceCertificate save(ComplianceCertificate certificate);

    Optional<ComplianceCertificate> findCurrent(UUID shipmentId);

    int highestVersionOf(UUID shipmentId);
}
