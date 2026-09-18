package com.coldchain.modules.identity.internal.domain.model;

import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.Scope;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Role {

    private final UUID id;

    private final RoleCode code;

    private final String name;

    private final boolean builtIn;

    private final Set<Scope> scopes;

    private Role(UUID id, RoleCode code, String name, boolean builtIn, Set<Scope> scopes) {
        this.id = Objects.requireNonNull(id);
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
        this.builtIn = builtIn;
        this.scopes = Set.copyOf(Objects.requireNonNull(scopes));
    }

    public static Role restore(UUID id, RoleCode code, String name, boolean builtIn, Set<Scope> scopes) {
        return new Role(id, code, name, builtIn, scopes);
    }

    public UUID id() {
        return id;
    }

    public RoleCode code() {
        return code;
    }

    public String name() {
        return name;
    }

    public boolean builtIn() {
        return builtIn;
    }

    public Set<Scope> scopes() {
        return scopes;
    }
}
