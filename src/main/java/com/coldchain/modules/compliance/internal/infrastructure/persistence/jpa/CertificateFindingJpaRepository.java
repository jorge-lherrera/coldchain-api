package com.coldchain.modules.compliance.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.compliance.internal.infrastructure.persistence.entity.CertificateFindingJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateFindingJpaRepository
        extends JpaRepository<CertificateFindingJpaEntity, UUID> {

    List<CertificateFindingJpaEntity> findByCertificateId(UUID certificateId);
}
