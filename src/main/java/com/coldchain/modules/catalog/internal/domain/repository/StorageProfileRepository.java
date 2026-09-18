package com.coldchain.modules.catalog.internal.domain.repository;

import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.Optional;
import java.util.UUID;

public interface StorageProfileRepository {

    StorageProfile save(StorageProfile profile);

    Optional<StorageProfile> findById(UUID id);

    PagedResult<StorageProfile> findByOrganization(UUID organizationId, PageCriteria criteria);
}
