package com.coldchain.modules.identity.internal.domain.repository;

import com.coldchain.modules.identity.internal.domain.model.ApiClient;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.Optional;
import java.util.UUID;

public interface ApiClientRepository {

    ApiClient save(ApiClient client);

    Optional<ApiClient> findById(UUID id);

    Optional<ApiClient> findByClientId(String clientId);

    PagedResult<ApiClient> findByOrganization(UUID organizationId, PageCriteria criteria);
}
