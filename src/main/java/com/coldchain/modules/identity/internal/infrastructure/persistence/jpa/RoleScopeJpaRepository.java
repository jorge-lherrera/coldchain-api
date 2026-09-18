package com.coldchain.modules.identity.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.RoleScopeId;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.RoleScopeJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleScopeJpaRepository extends JpaRepository<RoleScopeJpaEntity, RoleScopeId> {

    List<RoleScopeJpaEntity> findByRoleIdIn(Collection<UUID> roleIds);

    @Query("""
            select distinct s.scopeCode from RoleScopeJpaEntity s, UserRoleJpaEntity g
            where s.roleId = g.roleId and g.userId = :userId
            """)
    List<String> findScopeCodesOf(@Param("userId") UUID userId);
}
