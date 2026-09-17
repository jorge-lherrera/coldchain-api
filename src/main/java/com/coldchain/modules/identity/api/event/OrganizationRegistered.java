package com.coldchain.modules.identity.api.event;

import com.coldchain.modules.identity.api.ActorType;
import java.time.Instant;
import java.util.UUID;

public record OrganizationRegistered(UUID organizationId, ActorType actorType, UUID actorId,
        String taxId, Instant occurredAt) implements IdentityEvent {
}
