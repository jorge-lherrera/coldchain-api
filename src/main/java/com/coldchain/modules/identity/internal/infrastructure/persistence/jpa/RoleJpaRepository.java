package com.coldchain.modules.identity.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.RoleJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {

    Optional<RoleJpaEntity> findByCode(String code);

    @Query("""
            select r from RoleJpaEntity r, UserRoleJpaEntity g
            where g.roleId = r.id and g.userId = :userId
            """)
    List<RoleJpaEntity> findRolesOf(@Param("userId") UUID userId);
}
