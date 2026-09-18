package com.coldchain.delivery.web.identity.dto;

import java.util.UUID;

public record RegisterOrganizationResponse(UUID organizationId, UUID administratorId, String email) {
}
