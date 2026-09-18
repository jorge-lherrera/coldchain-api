package com.coldchain.modules.identity.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.OrganizationStatus;
import com.coldchain.modules.identity.internal.domain.model.Organization;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.OrganizationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OrganizationPersistenceMapper {

    public OrganizationJpaEntity toEntity(Organization organization) {
        return new OrganizationJpaEntity(organization.id(), organization.taxId(), organization.legalName(),
                organization.tradeName(), organization.kind().name(), organization.country(),
                organization.status().name(), organization.lockVersion());
    }

    public Organization toDomain(OrganizationJpaEntity entity) {
        return Organization.restore(entity.getId(), entity.getTaxId(), entity.getLegalName(),
                entity.getTradeName(), OrganizationKind.valueOf(entity.getKind()), entity.getCountry(),
                OrganizationStatus.valueOf(entity.getStatus()), entity.getLockVersion());
    }
}
