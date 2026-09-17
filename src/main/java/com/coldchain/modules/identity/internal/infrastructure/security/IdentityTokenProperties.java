package com.coldchain.modules.identity.internal.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coldchain.security")
public record IdentityTokenProperties(
        String jwtSecret,
        String issuer,
        Duration accessTokenLifetime,
        Duration refreshTokenLifetime,
        Duration activationLifetime) {

    public IdentityTokenProperties {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "coldchain.security.jwt-secret must be at least 32 characters and comes from the "
                            + "environment, never from a committed file");
        }
    }
}
