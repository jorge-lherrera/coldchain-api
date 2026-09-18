package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.Scope;
import java.util.Set;
import java.util.UUID;

public record EffectiveScopesResult(UUID userId, UUID organizationId, Set<Scope> scopes) {
}
