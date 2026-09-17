package com.coldchain.modules.identity.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.OrganizationJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, UUID> {

    Optional<OrganizationJpaEntity> findByTaxId(String taxId);
}
