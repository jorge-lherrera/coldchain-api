package com.coldchain.modules.catalog.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.catalog.internal.infrastructure.persistence.entity.StorageProfileJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageProfileJpaRepository extends JpaRepository<StorageProfileJpaEntity, UUID> {

    Page<StorageProfileJpaEntity> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId,
            Pageable pageable);
}
