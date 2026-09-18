package com.coldchain.modules.identity.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.ApiClientJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiClientJpaRepository extends JpaRepository<ApiClientJpaEntity, UUID> {

    Optional<ApiClientJpaEntity> findByClientId(String clientId);

    Page<ApiClientJpaEntity> findByOrganizationId(UUID organizationId, Pageable pageable);
}
