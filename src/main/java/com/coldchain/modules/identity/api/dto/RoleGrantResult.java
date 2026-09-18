package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.RoleCode;
import java.time.Instant;
import java.util.UUID;

public record RoleGrantResult(UUID userId, RoleCode role, UUID grantedBy, Instant grantedAt) {
}
