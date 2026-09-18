package com.coldchain.shared.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentActor {

    public Optional<UUID> id() {
        return claim(Jwt::getSubject);
    }

    public Optional<UUID> organizationId() {
        return claim(jwt -> jwt.getClaimAsString(TokenClaims.ORGANIZATION));
    }

    public Optional<String> actorType() {
        return jwt().map(token -> token.getClaimAsString(TokenClaims.ACTOR_TYPE));
    }

    public UUID requireOrganizationId() {
        return organizationId().orElseThrow(
                () -> new IllegalStateException("No organization in the security context"));
    }

    public UUID requireId() {
        return id().orElseThrow(() -> new IllegalStateException("No actor in the security context"));
    }

    private Optional<UUID> claim(java.util.function.Function<Jwt, String> reader) {
        return jwt().map(reader).map(UUID::fromString);
    }

    private static Optional<Jwt> jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt token)) {
            return Optional.empty();
        }
        return Optional.of(token);
    }
}
