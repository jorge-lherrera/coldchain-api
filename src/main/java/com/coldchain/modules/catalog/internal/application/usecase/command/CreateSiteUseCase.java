package com.coldchain.modules.catalog.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.dto.CreateSiteCommand;
import com.coldchain.modules.catalog.api.dto.SiteResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.model.Site;
import com.coldchain.modules.catalog.internal.domain.repository.SiteRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class CreateSiteUseCase {

    private final SiteRepository sites;

    private final CatalogApiMapper mapper;

    public CreateSiteUseCase(SiteRepository sites, CatalogApiMapper mapper) {
        this.sites = sites;
        this.mapper = mapper;
    }

    @Transactional
    public SiteResult execute(CreateSiteCommand command) {
        try {
            return mapper.toResult(sites.save(Site.createNew(command.organizationId(), command.code(),
                    command.name(), command.kind(), command.latitude(), command.longitude(),
                    command.timeZone())));
        } catch (IllegalArgumentException invalid) {
            throw DomainException.of(CatalogErrorCode.INVALID_TIME_ZONE, invalid.getMessage());
        }
    }
}
