package com.coldchain.modules.catalog.internal.application.usecase.query;

import com.coldchain.modules.catalog.api.dto.SiteResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.repository.SiteRepository;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;

@UseCase
public class ListSitesUseCase {

    private final SiteRepository sites;

    private final CatalogApiMapper mapper;

    public ListSitesUseCase(SiteRepository sites, CatalogApiMapper mapper) {
        this.sites = sites;
        this.mapper = mapper;
    }

    public PagedResult<SiteResult> execute(UUID organizationId, PageCriteria criteria) {
        return sites.findByOrganization(organizationId, criteria).map(mapper::toResult);
    }
}
