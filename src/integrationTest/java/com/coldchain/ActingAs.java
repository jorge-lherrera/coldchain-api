package com.coldchain;

import com.coldchain.shared.security.TokenClaims;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class ActingAs {

    private ActingAs() {
    }

    static <T> T user(UUID userId, UUID organizationId, Supplier<T> work) {
        Jwt token = Jwt.withTokenValue("integration")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim(TokenClaims.ORGANIZATION, organizationId.toString())
                .claim(TokenClaims.ACTOR_TYPE, "USER")
                .claim(TokenClaims.SCOPE, "USER_WRITE USER_READ ROLE_WRITE")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claims(claims -> claims.putAll(Map.of()))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
        try {
            return work.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
