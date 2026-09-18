package com.coldchain.modules.identity.internal.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RoleGrant(UUID userId, UUID roleId, UUID grantedBy, Instant grantedAt) {

    public RoleGrant {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(roleId);
        Objects.requireNonNull(grantedAt);
    }

    public static RoleGrant of(UUID userId, UUID roleId, UUID grantedBy, Instant grantedAt) {
        return new RoleGrant(userId, roleId, grantedBy, grantedAt);
    }
}
