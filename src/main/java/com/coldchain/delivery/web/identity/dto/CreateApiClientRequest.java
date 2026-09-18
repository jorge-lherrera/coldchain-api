package com.coldchain.delivery.web.identity.dto;

import com.coldchain.modules.identity.api.Scope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateApiClientRequest(
        @NotBlank @Size(max = 120) String label,
        @NotEmpty Set<Scope> scopes) {
}
