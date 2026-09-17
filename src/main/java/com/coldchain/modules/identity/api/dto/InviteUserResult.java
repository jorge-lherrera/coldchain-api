package com.coldchain.modules.identity.api.dto;

import java.time.Instant;
import java.util.UUID;

public record InviteUserResult(UUID userId, String email, String activationToken, Instant expiresAt) {
}
