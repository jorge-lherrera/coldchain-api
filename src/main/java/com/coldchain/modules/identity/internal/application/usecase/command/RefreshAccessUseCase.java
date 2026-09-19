package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.api.dto.RefreshAccessCommand;
import com.coldchain.modules.identity.api.dto.TokenResult;
import com.coldchain.modules.identity.api.event.AccessRefreshed;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.model.RefreshToken;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.repository.RefreshTokenRepository;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.domain.service.AccessTokenIssuer;
import com.coldchain.modules.identity.internal.domain.service.IssuedSecret;
import com.coldchain.modules.identity.internal.domain.service.OpaqueTokenFactory;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RefreshAccessUseCase {

    private final RefreshTokenRepository refreshTokens;

    private final RevokeTokenFamilyUseCase revokeTokenFamily;

    private final AppUserRepository users;

    private final RoleRepository roles;

    private final OpaqueTokenFactory tokens;

    private final AccessTokenIssuer accessTokens;

    private final ApplicationEventPublisher events;

    private final Clock clock;

    public RefreshAccessUseCase(RefreshTokenRepository refreshTokens,
            RevokeTokenFamilyUseCase revokeTokenFamily, AppUserRepository users, RoleRepository roles,
            OpaqueTokenFactory tokens, AccessTokenIssuer accessTokens, ApplicationEventPublisher events,
            Clock clock) {
        this.refreshTokens = refreshTokens;
        this.revokeTokenFamily = revokeTokenFamily;
        this.users = users;
        this.roles = roles;
        this.tokens = tokens;
        this.accessTokens = accessTokens;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public TokenResult execute(RefreshAccessCommand command) {
        Instant now = clock.instant();
        RefreshToken presented = refreshTokens.findByTokenHash(tokens.fingerprint(command.refreshToken()))
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.REFRESH_TOKEN_INVALID));
        if (presented.spent()) {
            revokeTokenFamily.execute(presented);
            throw DomainException.of(IdentityErrorCode.REFRESH_TOKEN_REUSED);
        }
        if (presented.revoked()) {
            throw DomainException.of(IdentityErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (presented.expiredAt(now)) {
            throw DomainException.of(IdentityErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        AppUser user = users.findById(presented.appUserId())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.USER_NOT_FOUND));
        if (!user.active()) {
            throw DomainException.of(IdentityErrorCode.USER_NOT_ACTIVE);
        }
        refreshTokens.save(presented.markUsed(now));
        IssuedSecret next = tokens.issue();
        refreshTokens.save(presented.rotateInto(next.fingerprint(), now,
                accessTokens.refreshTokenLifetime()));
        Set<Scope> scopes = roles.findScopesOf(user.id());
        String accessToken = accessTokens.issue(ActorType.USER, user.id(), user.organizationId(), scopes);
        events.publishEvent(new AccessRefreshed(user.organizationId(), ActorType.USER, user.id(),
                presented.familyId(), now));
        return new TokenResult(accessToken, next.plainToken(), "Bearer",
                accessTokens.accessTokenLifetime().toSeconds());
    }
}
