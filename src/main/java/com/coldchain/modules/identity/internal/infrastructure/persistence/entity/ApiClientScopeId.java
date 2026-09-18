package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

public record ApiClientScopeId(UUID apiClientId, String scopeCode) implements Serializable {

    public ApiClientScopeId() {
        this(null, null);
    }
}
