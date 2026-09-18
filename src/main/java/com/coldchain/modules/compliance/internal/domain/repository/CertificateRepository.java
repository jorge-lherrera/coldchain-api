package com.coldchain.modules.compliance.internal.domain.repository;

import com.coldchain.modules.compliance.internal.domain.model.Certificate;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository {

    Certificate save(Certificate certificate);

    Optional<Certificate> findCurrent(UUID shipmentId);

    int highestVersionOf(UUID shipmentId);
}
