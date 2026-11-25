package com.coldchain.modules.identity.internal.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coldchain.platform")
public record PlatformClientProperties(String clientId, String clientSecretHash) {

    private static final String ARGON2ID_PREFIX = "$argon2id$";

    public PlatformClientProperties {
        boolean named = clientId != null && !clientId.isBlank();
        boolean hashed = clientSecretHash != null && !clientSecretHash.isBlank();
        if (named != hashed) {
            throw new IllegalStateException(
                    "coldchain.platform needs both client-id and client-secret-hash, or neither");
        }
        if (hashed && !clientSecretHash.startsWith(ARGON2ID_PREFIX)) {
            throw new IllegalStateException(
                    "coldchain.platform.client-secret-hash takes the Argon2id hash of the secret, never the secret");
        }
    }

    public boolean configured() {
        return clientId != null && !clientId.isBlank();
    }
}
