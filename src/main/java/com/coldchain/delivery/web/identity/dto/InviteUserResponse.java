package com.coldchain.delivery.web.identity.dto;

import java.time.Instant;
import java.util.UUID;

public record InviteUserResponse(UUID userId, String email, String activationToken, Instant expiresAt) {
}
