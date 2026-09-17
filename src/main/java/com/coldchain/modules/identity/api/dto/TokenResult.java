package com.coldchain.modules.identity.api.dto;

public record TokenResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds) {
}
