package com.coldchain.modules.identity.api.event;

import com.coldchain.modules.identity.api.ActorType;
import java.time.Instant;
import java.util.UUID;

public sealed interface IdentityEvent permits
        OrganizationRegistered,
        UserInvited,
        UserActivated,
        UserAuthenticated,
        AccessRefreshed,
        RefreshTokenReuseDetected,
        ClientTokenIssued,
        ApiClientCreated,
        RoleAssigned,
        RoleRevoked {

    UUID organizationId();

    ActorType actorType();

    UUID actorId();

    Instant occurredAt();
}
