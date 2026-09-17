package com.coldchain.modules.identity.api.event;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.RoleCode;
import java.time.Instant;
import java.util.UUID;

public record UserInvited(UUID organizationId, ActorType actorType, UUID actorId, UUID invitedUserId,
        RoleCode role, Instant occurredAt) implements IdentityEvent {
}
