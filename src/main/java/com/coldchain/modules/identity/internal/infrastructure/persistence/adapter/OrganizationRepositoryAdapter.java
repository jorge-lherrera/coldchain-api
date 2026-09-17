package com.coldchain.modules.identity.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.identity.internal.domain.model.Organization;
import com.coldchain.modules.identity.internal.domain.repository.OrganizationRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.OrganizationJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.mapper.OrganizationPersistenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final OrganizationJpaRepository repository;

    private final OrganizationPersistenceMapper mapper;

    public OrganizationRepositoryAdapter(OrganizationJpaRepository repository,
            OrganizationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Organization save(Organization organization) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(organization)));
        } catch (DataIntegrityViolationException cause) {
            throw ConstraintTranslation.translate(cause);
        }
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Organization> findByTaxId(String taxId) {
        return repository.findByTaxId(taxId).map(mapper::toDomain);
    }
}
