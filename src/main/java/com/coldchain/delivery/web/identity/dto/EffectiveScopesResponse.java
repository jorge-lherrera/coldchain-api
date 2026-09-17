package com.coldchain.delivery.web.identity.dto;

import com.coldchain.modules.identity.api.Scope;
import java.util.Set;
import java.util.UUID;

public record EffectiveScopesResponse(UUID userId, UUID organizationId, Set<Scope> scopes) {
}
