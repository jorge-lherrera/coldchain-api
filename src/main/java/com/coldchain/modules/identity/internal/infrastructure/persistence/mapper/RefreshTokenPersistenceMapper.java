package com.coldchain.modules.identity.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.identity.internal.domain.model.RefreshToken;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenPersistenceMapper {

    public RefreshTokenJpaEntity toEntity(RefreshToken token) {
        return new RefreshTokenJpaEntity(token.id(), token.organizationId(), token.appUserId(),
                token.familyId(), token.tokenHash(), token.issuedAt(), token.expiresAt(), token.usedAt(),
                token.revokedAt());
    }

    public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.restore(entity.getId(), entity.getOrganizationId(), entity.getAppUserId(),
                entity.getFamilyId(), entity.getTokenHash(), entity.getIssuedAt(), entity.getExpiresAt(),
                entity.getUsedAt(), entity.getRevokedAt());
    }
}
