package com.coldchain.modules.identity.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.identity.api.UserStatus;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.AppUserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AppUserPersistenceMapper {

    public AppUserJpaEntity toEntity(AppUser user) {
        return new AppUserJpaEntity(user.id(), user.organizationId(), user.email(), user.passwordHash(),
                user.fullName(), user.status().name(), user.activationTokenHash(), user.activationExpiresAt(),
                user.lastLoginAt(), user.lockVersion());
    }

    public AppUser toDomain(AppUserJpaEntity entity) {
        return AppUser.restore(entity.getId(), entity.getOrganizationId(), entity.getEmail(),
                entity.getPasswordHash(), entity.getFullName(), UserStatus.valueOf(entity.getStatus()),
                entity.getActivationTokenHash(), entity.getActivationExpiresAt(), entity.getLastLoginAt(), entity.getLockVersion());
    }
}
