package com.coldchain.modules.catalog.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.catalog.api.ProfileStatus;
import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.modules.catalog.internal.domain.model.Thresholds;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.entity.StorageProfileJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class StorageProfilePersistenceMapper {

    public StorageProfileJpaEntity toEntity(StorageProfile profile) {
        Thresholds thresholds = profile.thresholds();
        return new StorageProfileJpaEntity(profile.id(), profile.organizationId(), profile.code(),
                profile.name(), profile.version(), profile.status().name(), thresholds.minCelsius(),
                thresholds.maxCelsius(), thresholds.maxSingleExcursionMinutes(),
                thresholds.maxCumulativeExcursionMinutes(), thresholds.minCoveragePercent(),
                profile.deletedAt(), profile.lockVersion());
    }

    public StorageProfile toDomain(StorageProfileJpaEntity entity) {
        return StorageProfile.restore(entity.getId(), entity.getOrganizationId(), entity.getCode(),
                entity.getName(), entity.getProfileVersion(),
                ProfileStatus.valueOf(entity.getStatus()),
                new Thresholds(entity.getMinCelsius(), entity.getMaxCelsius(),
                        entity.getMaxSingleExcursionMinutes(), entity.getMaxCumulativeExcursionMinutes(),
                        entity.getMinCoveragePercent()),
                entity.getDeletedAt(), entity.getLockVersion());
    }
}
