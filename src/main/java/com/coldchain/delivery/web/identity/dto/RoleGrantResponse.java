package com.coldchain.delivery.web.identity.dto;

import com.coldchain.modules.identity.api.RoleCode;
import java.time.Instant;
import java.util.UUID;

public record RoleGrantResponse(UUID userId, RoleCode role, UUID grantedBy, Instant grantedAt) {
}
