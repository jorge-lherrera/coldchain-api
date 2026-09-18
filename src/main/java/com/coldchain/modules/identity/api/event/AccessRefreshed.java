package com.coldchain.modules.identity.api.event;

import com.coldchain.modules.identity.api.ActorType;
import java.time.Instant;
import java.util.UUID;

public record AccessRefreshed(UUID organizationId, ActorType actorType, UUID actorId, UUID familyId,
        Instant occurredAt) implements IdentityEvent {
}
