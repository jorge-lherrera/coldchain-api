package com.coldchain.modules.catalog.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.modules.catalog.internal.domain.repository.StorageProfileRepository;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.jpa.StorageProfileJpaRepository;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.mapper.StorageProfilePersistenceMapper;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import com.coldchain.shared.paging.SpringDataPaging;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class StorageProfileRepositoryAdapter implements StorageProfileRepository {

    private final StorageProfileJpaRepository profiles;

    private final StorageProfilePersistenceMapper mapper;

    public StorageProfileRepositoryAdapter(StorageProfileJpaRepository profiles,
            StorageProfilePersistenceMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @Override
    public StorageProfile save(StorageProfile profile) {
        try {
            profiles.saveAndFlush(mapper.toEntity(profile));
        } catch (DataIntegrityViolationException cause) {
            throw CatalogConstraintTranslation.translate(cause);
        }
        return profile;
    }

    @Override
    public Optional<StorageProfile> findById(UUID id) {
        return profiles.findById(id).map(mapper::toDomain);
    }

    @Override
    public PagedResult<StorageProfile> findByOrganization(UUID organizationId, PageCriteria criteria) {
        return SpringDataPaging.toPagedResult(
                profiles.findByOrganizationIdAndDeletedAtIsNull(organizationId,
                        SpringDataPaging.toPageable(criteria)).map(mapper::toDomain),
                criteria);
    }
}
