package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

public record RoleScopeId(UUID roleId, String scopeCode) implements Serializable {

    public RoleScopeId() {
        this(null, null);
    }
}
