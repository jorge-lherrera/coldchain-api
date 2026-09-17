package com.coldchain.modules.identity.api.event;

import com.coldchain.modules.identity.api.ActorType;
import java.time.Instant;
import java.util.UUID;

public record UserActivated(UUID organizationId, ActorType actorType, UUID actorId,
        Instant occurredAt) implements IdentityEvent {
}
