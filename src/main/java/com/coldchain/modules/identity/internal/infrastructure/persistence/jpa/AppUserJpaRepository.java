package com.coldchain.modules.identity.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.AppUserJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, UUID> {

    @Query("select u from AppUserJpaEntity u where lower(u.email) = lower(:email)")
    Optional<AppUserJpaEntity> findByEmailIgnoringCase(@Param("email") String email);

    Optional<AppUserJpaEntity> findByActivationTokenHash(String activationTokenHash);

    Page<AppUserJpaEntity> findByOrganizationId(UUID organizationId, Pageable pageable);

    @Query("""
            select count(u) from AppUserJpaEntity u
            where u.organizationId = :organizationId
              and u.status = 'ACTIVE'
              and exists (
                  select 1 from UserRoleJpaEntity g, RoleJpaEntity r
                  where g.userId = u.id and g.roleId = r.id and r.code = 'ORG_ADMIN')
            """)
    long countAdministrators(@Param("organizationId") UUID organizationId);
}
