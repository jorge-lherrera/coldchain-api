package com.coldchain.modules.identity.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.AppUserJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.mapper.AppUserPersistenceMapper;
import com.coldchain.shared.pagination.PageCriteria;
import com.coldchain.shared.pagination.PagedResult;
import com.coldchain.shared.pagination.SpringDataPaging;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class AppUserRepositoryAdapter implements AppUserRepository {

    private final AppUserJpaRepository repository;

    private final AppUserPersistenceMapper mapper;

    public AppUserRepositoryAdapter(AppUserJpaRepository repository, AppUserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AppUser save(AppUser user) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(user)));
        } catch (DataIntegrityViolationException cause) {
            throw ConstraintTranslation.translate(cause);
        }
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return repository.findByEmailIgnoringCase(email).map(mapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByActivationTokenHash(String activationTokenHash) {
        return repository.findByActivationTokenHash(activationTokenHash).map(mapper::toDomain);
    }

    @Override
    public PagedResult<AppUser> findByOrganization(UUID organizationId, PageCriteria criteria) {
        return SpringDataPaging.toPagedResult(
                repository.findByOrganizationId(organizationId, SpringDataPaging.toPageable(criteria))
                        .map(mapper::toDomain),
                criteria);
    }

    @Override
    public long countAdministrators(UUID organizationId) {
        return repository.countAdministrators(organizationId);
    }
}
