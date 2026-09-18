package com.coldchain.modules.catalog.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.catalog.internal.infrastructure.persistence.entity.ProductJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Page<ProductJpaEntity> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    long countByStorageProfileIdAndDeletedAtIsNull(UUID storageProfileId);
}
