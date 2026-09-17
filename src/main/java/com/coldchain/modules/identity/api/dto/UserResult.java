package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResult(
        UUID id,
        UUID organizationId,
        String email,
        String fullName,
        UserStatus status,
        Instant lastLoginAt) {
}
