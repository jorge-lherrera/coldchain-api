package com.coldchain.modules.identity.internal.domain.model;

import com.coldchain.modules.identity.api.Scope;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PlatformClient(String clientId, String secretHash) {

    private static final Set<Scope> SCOPES = Set.of(Scope.PLATFORM_EXCURSION_READ);

    public PlatformClient {
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(secretHash);
    }

    public UUID actorId() {
        return UUID.nameUUIDFromBytes(("platform:" + clientId).getBytes(StandardCharsets.UTF_8));
    }

    public Set<Scope> scopes() {
        return SCOPES;
    }
}
