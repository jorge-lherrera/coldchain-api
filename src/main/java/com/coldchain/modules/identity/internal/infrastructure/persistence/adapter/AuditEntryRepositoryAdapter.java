package com.coldchain.modules.identity.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.identity.internal.domain.model.AuditEntry;
import com.coldchain.modules.identity.internal.domain.repository.AuditEntryRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.AuditEntryJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.mapper.AuditEntryPersistenceMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuditEntryRepositoryAdapter implements AuditEntryRepository {

    private final AuditEntryJpaRepository repository;

    private final AuditEntryPersistenceMapper mapper;

    public AuditEntryRepositoryAdapter(AuditEntryJpaRepository repository,
            AuditEntryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AuditEntry save(AuditEntry entry) {
        repository.saveAndFlush(mapper.toEntity(entry));
        return entry;
    }
}
