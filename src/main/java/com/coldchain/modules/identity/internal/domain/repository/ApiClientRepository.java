package com.coldchain.modules.identity.internal.domain.repository;

import com.coldchain.modules.identity.internal.domain.model.ApiClient;
import java.util.Optional;
import java.util.UUID;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;

public interface ApiClientRepository {

    ApiClient save(ApiClient client);

    Optional<ApiClient> findById(UUID id);

    Optional<ApiClient> findByClientId(String clientId);

    PagedResult<ApiClient> findByOrganization(UUID organizationId, PageCriteria criteria);
}
