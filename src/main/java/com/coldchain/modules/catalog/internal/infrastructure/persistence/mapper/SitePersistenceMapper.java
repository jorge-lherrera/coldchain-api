package com.coldchain.modules.catalog.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.catalog.api.SiteKind;
import com.coldchain.modules.catalog.internal.domain.model.Site;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.entity.SiteJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SitePersistenceMapper {

    public SiteJpaEntity toEntity(Site site) {
        return new SiteJpaEntity(site.id(), site.organizationId(), site.code(), site.name(),
                site.kind().name(), site.latitude(), site.longitude(), site.timeZone(), site.deletedAt(), site.lockVersion());
    }

    public Site toDomain(SiteJpaEntity entity) {
        return Site.restore(entity.getId(), entity.getOrganizationId(), entity.getCode(), entity.getName(),
                SiteKind.valueOf(entity.getKind()), entity.getLatitude(), entity.getLongitude(),
                entity.getTimeZone(), entity.getDeletedAt(), entity.getLockVersion());
    }
}
