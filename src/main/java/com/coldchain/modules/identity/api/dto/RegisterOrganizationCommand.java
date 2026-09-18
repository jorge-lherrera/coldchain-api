package com.coldchain.modules.identity.api.dto;

import com.coldchain.modules.identity.api.OrganizationKind;

public record RegisterOrganizationCommand(
        String taxId,
        String legalName,
        String tradeName,
        OrganizationKind kind,
        String country,
        String administratorEmail,
        String administratorFullName,
        String administratorPassword) {
}
