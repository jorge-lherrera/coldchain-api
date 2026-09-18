package com.coldchain.modules.catalog.internal.application.usecase.query;

import com.coldchain.modules.catalog.api.dto.SiteResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.repository.SiteRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.util.UUID;

@UseCase
public class GetSiteUseCase {

    private final SiteRepository sites;

    private final CatalogApiMapper mapper;

    public GetSiteUseCase(SiteRepository sites, CatalogApiMapper mapper) {
        this.sites = sites;
        this.mapper = mapper;
    }

    public SiteResult execute(UUID siteId) {
        return sites.findById(siteId).map(mapper::toResult)
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.SITE_NOT_FOUND));
    }
}
