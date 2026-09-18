package com.coldchain.modules.identity.api.dto;

import java.util.UUID;

public record ActivateUserResult(UUID userId, String email) {
}
