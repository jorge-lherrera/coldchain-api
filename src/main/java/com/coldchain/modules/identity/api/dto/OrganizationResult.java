package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.OrganizationStatus;
import java.util.UUID;

public record OrganizationResult(
        UUID id,
        String taxId,
        String legalName,
        String tradeName,
        OrganizationKind kind,
        String country,
        OrganizationStatus status) {
}
