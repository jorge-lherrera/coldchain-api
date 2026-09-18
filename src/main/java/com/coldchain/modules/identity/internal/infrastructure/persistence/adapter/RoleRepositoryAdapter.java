package com.coldchain.modules.identity.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.internal.domain.model.Role;
import com.coldchain.modules.identity.internal.domain.model.RoleGrant;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.RoleJpaEntity;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.RoleScopeJpaEntity;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.UserRoleJpaEntity;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.RoleJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.RoleScopeJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.UserRoleJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.mapper.RolePersistenceMapper;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository roles;

    private final RoleScopeJpaRepository roleScopes;

    private final UserRoleJpaRepository grants;

    private final RolePersistenceMapper mapper;

    public RoleRepositoryAdapter(RoleJpaRepository roles, RoleScopeJpaRepository roleScopes,
            UserRoleJpaRepository grants, RolePersistenceMapper mapper) {
        this.roles = roles;
        this.roleScopes = roleScopes;
        this.grants = grants;
        this.mapper = mapper;
    }

    @Override
    public List<Role> findAll() {
        return withScopes(roles.findAll());
    }

    @Override
    public Optional<Role> findByCode(RoleCode code) {
        return roles.findByCode(code.name()).map(entity -> withScopes(List.of(entity)).getFirst());
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return roles.findById(id).map(entity -> withScopes(List.of(entity)).getFirst());
    }

    @Override
    public List<Role> findRolesOf(UUID userId) {
        return withScopes(roles.findRolesOf(userId));
    }

    @Override
    public Set<Scope> findScopesOf(UUID userId) {
        return toScopes(roleScopes.findScopeCodesOf(userId));
    }

    @Override
    public RoleGrant grant(RoleGrant grant) {
        grants.saveAndFlush(new UserRoleJpaEntity(grant.userId(), grant.roleId(), grant.grantedBy(),
                grant.grantedAt()));
        return grant;
    }

    @Override
    public boolean revoke(UUID userId, UUID roleId) {
        return grants.deleteByUserIdAndRoleId(userId, roleId) > 0;
    }

    @Override
    public boolean granted(UUID userId, UUID roleId) {
        return grants.existsByUserIdAndRoleId(userId, roleId);
    }

    private List<Role> withScopes(List<RoleJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<String>> codesByRole = roleScopes
                .findByRoleIdIn(entities.stream().map(RoleJpaEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(RoleScopeJpaEntity::getRoleId,
                        Collectors.mapping(RoleScopeJpaEntity::getScopeCode, Collectors.toList())));
        return entities.stream()
                .map(entity -> mapper.toDomain(entity, toScopes(codesByRole.getOrDefault(entity.getId(),
                        List.of()))))
                .toList();
    }

    private static Set<Scope> toScopes(Collection<String> codes) {
        Set<Scope> scopes = EnumSet.noneOf(Scope.class);
        codes.stream().map(Scope::ofCode).flatMap(Optional::stream).forEach(scopes::add);
        return Set.copyOf(scopes);
    }
}
