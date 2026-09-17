package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.Scope;
import java.util.Set;
import java.util.UUID;

public record CreateApiClientCommand(UUID organizationId, String label, Set<Scope> scopes) {
}
