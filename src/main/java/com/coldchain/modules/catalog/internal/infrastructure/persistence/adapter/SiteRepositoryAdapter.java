package com.coldchain.modules.catalog.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.catalog.internal.domain.model.Site;
import com.coldchain.modules.catalog.internal.domain.repository.SiteRepository;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.jpa.SiteJpaRepository;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.mapper.SitePersistenceMapper;
import com.coldchain.shared.pagination.PageCriteria;
import com.coldchain.shared.pagination.PagedResult;
import com.coldchain.shared.pagination.SpringDataPaging;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class SiteRepositoryAdapter implements SiteRepository {

    private final SiteJpaRepository sites;

    private final SitePersistenceMapper mapper;

    public SiteRepositoryAdapter(SiteJpaRepository sites, SitePersistenceMapper mapper) {
        this.sites = sites;
        this.mapper = mapper;
    }

    @Override
    public Site save(Site site) {
        try {
            sites.saveAndFlush(mapper.toEntity(site));
        } catch (DataIntegrityViolationException cause) {
            throw CatalogConstraintTranslation.translate(cause);
        }
        return site;
    }

    @Override
    public Optional<Site> findById(UUID id) {
        return sites.findById(id).map(mapper::toDomain);
    }

    @Override
    public PagedResult<Site> findByOrganization(UUID organizationId, PageCriteria criteria) {
        return SpringDataPaging.toPagedResult(
                sites.findByOrganizationIdAndDeletedAtIsNull(organizationId,
                        SpringDataPaging.toPageable(criteria)).map(mapper::toDomain),
                criteria);
    }
}
