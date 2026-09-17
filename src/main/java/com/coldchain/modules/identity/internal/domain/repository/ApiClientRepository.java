package com.coldchain.modules.identity.internal.domain.repository;

import com.coldchain.modules.identity.internal.domain.model.ApiClient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApiClientRepository {

    ApiClient save(ApiClient client);

    Optional<ApiClient> findById(UUID id);

    Optional<ApiClient> findByClientId(String clientId);

    Page<ApiClient> findByOrganization(UUID organizationId, Pageable pageable);
}
