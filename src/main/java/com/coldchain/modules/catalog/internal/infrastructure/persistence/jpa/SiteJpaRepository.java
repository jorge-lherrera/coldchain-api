package com.coldchain.modules.catalog.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.catalog.internal.infrastructure.persistence.entity.SiteJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteJpaRepository extends JpaRepository<SiteJpaEntity, UUID> {

    Page<SiteJpaEntity> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);
}
