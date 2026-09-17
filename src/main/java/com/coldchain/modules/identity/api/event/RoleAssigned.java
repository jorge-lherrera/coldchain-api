package com.coldchain.modules.identity.api.event;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.RoleCode;
import java.time.Instant;
import java.util.UUID;

public record RoleAssigned(UUID organizationId, ActorType actorType, UUID actorId, UUID subjectId,
        RoleCode role, Instant occurredAt) implements IdentityEvent {
}
