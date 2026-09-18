package com.coldchain.delivery.web.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateUserRequest(
        @NotBlank String activationToken,
        @NotBlank @Size(min = 12, max = 128) String password) {
}
