package com.coldchain.modules.identity.internal.infrastructure.security;

import com.coldchain.modules.identity.internal.domain.service.SecretHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2SecretHasher implements SecretHasher {

    private final PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String hash(String plainSecret) {
        return encoder.encode(plainSecret);
    }

    @Override
    public boolean matches(String plainSecret, String hash) {
        return hash != null && encoder.matches(plainSecret, hash);
    }
}
