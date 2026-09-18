package com.coldchain.modules.identity.internal.infrastructure.security;

import com.coldchain.modules.identity.internal.domain.service.IssuedSecret;
import com.coldchain.modules.identity.internal.domain.service.OpaqueTokenFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RandomOpaqueTokenFactory implements OpaqueTokenFactory {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    private final IdentityTokenProperties properties;

    public RandomOpaqueTokenFactory(IdentityTokenProperties properties) {
        this.properties = properties;
    }

    @Override
    public IssuedSecret issue() {
        byte[] material = new byte[TOKEN_BYTES];
        random.nextBytes(material);
        String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(material);
        return new IssuedSecret(plainToken, fingerprint(plainToken));
    }

    @Override
    public String fingerprint(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(plainToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", cause);
        }
    }

    @Override
    public Duration activationLifetime() {
        return properties.activationLifetime();
    }
}
