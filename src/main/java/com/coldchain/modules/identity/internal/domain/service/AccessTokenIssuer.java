package com.coldchain.modules.identity.internal.domain.service;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.Scope;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public interface AccessTokenIssuer {

    String issue(ActorType actorType, UUID actorId, UUID organizationId, Set<Scope> scopes);

    String issueForPlatform(UUID actorId, Set<Scope> scopes);

    Duration accessTokenLifetime();

    Duration refreshTokenLifetime();
}
