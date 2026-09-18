package com.coldchain.modules.identity.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.ApiClientScopeId;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.ApiClientScopeJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiClientScopeJpaRepository
        extends JpaRepository<ApiClientScopeJpaEntity, ApiClientScopeId> {

    List<ApiClientScopeJpaEntity> findByApiClientId(UUID apiClientId);

    void deleteByApiClientId(UUID apiClientId);
}
