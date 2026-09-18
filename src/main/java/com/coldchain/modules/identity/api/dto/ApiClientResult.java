package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.ApiClientStatus;
import com.coldchain.modules.identity.api.Scope;
import java.util.Set;
import java.util.UUID;

public record ApiClientResult(
        UUID id,
        String clientId,
        String clientSecret,
        String label,
        ApiClientStatus status,
        Set<Scope> scopes) {
}
