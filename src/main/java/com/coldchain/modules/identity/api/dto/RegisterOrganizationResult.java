package com.coldchain.modules.identity.api.dto;

import java.util.UUID;

public record RegisterOrganizationResult(UUID organizationId, UUID administratorId, String email) {
}
