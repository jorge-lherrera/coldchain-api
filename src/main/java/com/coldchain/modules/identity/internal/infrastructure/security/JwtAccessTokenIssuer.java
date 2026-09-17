package com.coldchain.modules.identity.internal.infrastructure.security;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.internal.domain.service.AccessTokenIssuer;
import com.coldchain.shared.security.TokenClaims;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;

    private final IdentityTokenProperties properties;

    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder encoder, IdentityTokenProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String issue(ActorType actorType, UUID actorId, UUID organizationId, Set<Scope> scopes) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenLifetime()))
                .subject(actorId.toString())
                .claim(TokenClaims.ORGANIZATION, organizationId.toString())
                .claim(TokenClaims.ACTOR_TYPE, actorType.name())
                .claim(TokenClaims.SCOPE, scopes.stream().map(Scope::name).sorted()
                        .collect(Collectors.joining(" ")))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public Duration accessTokenLifetime() {
        return properties.accessTokenLifetime();
    }

    @Override
    public Duration refreshTokenLifetime() {
        return properties.refreshTokenLifetime();
    }
}
