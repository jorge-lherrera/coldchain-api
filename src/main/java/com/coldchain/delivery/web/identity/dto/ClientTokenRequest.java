package com.coldchain.delivery.web.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientTokenRequest(@NotBlank String clientId, @NotBlank String clientSecret) {
}
