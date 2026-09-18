package com.coldchain.modules.identity.api.event;

import com.coldchain.modules.identity.api.ActorType;
import java.time.Instant;
import java.util.UUID;

public record RefreshTokenReuseDetected(UUID organizationId, ActorType actorType, UUID actorId,
        UUID familyId, int revokedTokens, Instant occurredAt) implements IdentityEvent {
}
