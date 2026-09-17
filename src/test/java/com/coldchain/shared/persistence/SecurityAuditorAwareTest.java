package com.coldchain.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anUnauthenticatedWriteIsAttributedToTheSystemActorAndNeverToNobody() {
        assertThat(auditorAware.getCurrentAuditor()).contains(SecurityAuditorAware.SYSTEM);
    }

    @Test
    void anAuthenticatedWriteIsAttributedToTheUserBehindIt() {
        UUID actor = UUID.fromString("0192f0a1-2b3c-7d4e-8f01-23456789abcd");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(actor, null, List.of()));

        assertThat(auditorAware.getCurrentAuditor()).contains(actor);
    }

    @Test
    void anAnonymousPrincipalDoesNotBecomeAFabricatedUser() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(auditorAware.getCurrentAuditor()).contains(SecurityAuditorAware.SYSTEM);
    }
}
