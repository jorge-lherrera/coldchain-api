package com.coldchain.delivery.web.identity.dto;

import java.util.UUID;

public record ActivateUserResponse(UUID userId, String email) {
}
