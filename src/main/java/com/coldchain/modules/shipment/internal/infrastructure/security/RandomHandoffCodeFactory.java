package com.coldchain.modules.shipment.internal.infrastructure.security;

import com.coldchain.modules.shipment.internal.domain.service.HandoffCodeFactory;
import com.coldchain.modules.shipment.internal.domain.service.IssuedCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RandomHandoffCodeFactory implements HandoffCodeFactory {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    private final HandoffProperties properties;

    public RandomHandoffCodeFactory(HandoffProperties properties) {
        this.properties = properties;
    }

    @Override
    public IssuedCode issue() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int position = 0; position < CODE_LENGTH; position++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        String plainCode = code.toString();
        return new IssuedCode(plainCode, fingerprint(plainCode));
    }

    @Override
    public String fingerprint(String plainCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(plainCode.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", cause);
        }
    }

    @Override
    public Duration lifetime() {
        return properties.codeLifetime();
    }
}
