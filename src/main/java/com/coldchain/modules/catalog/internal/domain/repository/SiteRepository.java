package com.coldchain.modules.catalog.internal.domain.repository;

import com.coldchain.modules.catalog.internal.domain.model.Site;
import com.coldchain.shared.pagination.PageCriteria;
import com.coldchain.shared.pagination.PagedResult;
import java.util.Optional;
import java.util.UUID;

public interface SiteRepository {

    Site save(Site site);

    Optional<Site> findById(UUID id);

    PagedResult<Site> findByOrganization(UUID organizationId, PageCriteria criteria);
}
