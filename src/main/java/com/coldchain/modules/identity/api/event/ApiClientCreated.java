package com.coldchain.modules.identity.api.event;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.Scope;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ApiClientCreated(UUID organizationId, ActorType actorType, UUID actorId, UUID apiClientId,
        String label, Set<Scope> scopes, Instant occurredAt) implements IdentityEvent {
}
