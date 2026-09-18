package com.coldchain.modules.identity.internal.domain.repository;

import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.internal.domain.model.Role;
import com.coldchain.modules.identity.internal.domain.model.RoleGrant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RoleRepository {

    List<Role> findAll();

    Optional<Role> findByCode(RoleCode code);

    Optional<Role> findById(UUID id);

    List<Role> findRolesOf(UUID userId);

    Set<Scope> findScopesOf(UUID userId);

    RoleGrant grant(RoleGrant grant);

    boolean revoke(UUID userId, UUID roleId);

    boolean granted(UUID userId, UUID roleId);
}
