package com.coldchain.modules.catalog.internal.application.usecase.query;

import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.repository.StorageProfileRepository;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;

@UseCase
public class ListStorageProfilesUseCase {

    private final StorageProfileRepository profiles;

    private final CatalogApiMapper mapper;

    public ListStorageProfilesUseCase(StorageProfileRepository profiles, CatalogApiMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    public PagedResult<StorageProfileResult> execute(UUID organizationId, PageCriteria criteria) {
        return profiles.findByOrganization(organizationId, criteria).map(mapper::toResult);
    }
}
