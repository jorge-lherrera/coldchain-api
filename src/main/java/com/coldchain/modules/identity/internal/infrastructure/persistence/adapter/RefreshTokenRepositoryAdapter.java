package com.coldchain.modules.identity.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.identity.internal.domain.model.RefreshToken;
import com.coldchain.modules.identity.internal.domain.repository.RefreshTokenRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.RefreshTokenJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.mapper.RefreshTokenPersistenceMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository repository;

    private final RefreshTokenPersistenceMapper mapper;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository repository,
            RefreshTokenPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public int revokeFamily(UUID familyId, Instant when) {
        return repository.revokeFamily(familyId, when);
    }
}
