package com.coldchain.modules.identity.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.UserRoleId;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.UserRoleJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UserRoleId> {

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    long deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
