package com.coldchain.delivery.web.identity.dto;

import com.coldchain.modules.identity.api.OrganizationKind;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterOrganizationRequest(
        @NotBlank @Size(max = 64) String taxId,
        @NotBlank @Size(max = 200) String legalName,
        @Size(max = 200) String tradeName,
        @NotNull OrganizationKind kind,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotBlank @Email @Size(max = 320) String administratorEmail,
        @NotBlank @Size(max = 200) String administratorFullName,
        @NotBlank @Size(min = 12, max = 128) String administratorPassword) {
}
