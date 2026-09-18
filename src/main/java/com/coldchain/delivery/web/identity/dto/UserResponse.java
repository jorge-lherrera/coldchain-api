package com.coldchain.delivery.web.identity.dto;

import com.coldchain.modules.identity.api.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID organizationId,
        String email,
        String fullName,
        UserStatus status,
        Instant lastLoginAt) {
}
