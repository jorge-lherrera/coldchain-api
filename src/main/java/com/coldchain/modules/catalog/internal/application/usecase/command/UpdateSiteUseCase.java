package com.coldchain.modules.catalog.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.dto.SiteResult;
import com.coldchain.modules.catalog.api.dto.UpdateSiteCommand;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.model.Site;
import com.coldchain.modules.catalog.internal.domain.repository.SiteRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class UpdateSiteUseCase {

    private final SiteRepository sites;

    private final CatalogApiMapper mapper;

    public UpdateSiteUseCase(SiteRepository sites, CatalogApiMapper mapper) {
        this.sites = sites;
        this.mapper = mapper;
    }

    @Transactional
    public SiteResult execute(UpdateSiteCommand command) {
        Site site = sites.findById(command.siteId())
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.SITE_NOT_FOUND));
        try {
            return mapper.toResult(sites.save(site.relocatedTo(command.name(), command.latitude(),
                    command.longitude(), command.timeZone())));
        } catch (IllegalArgumentException invalid) {
            throw DomainException.of(CatalogErrorCode.INVALID_TIME_ZONE, invalid.getMessage());
        }
    }
}
