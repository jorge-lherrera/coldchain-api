package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.api.dto.AuthenticateCommand;
import com.coldchain.modules.identity.api.dto.TokenResult;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.model.Organization;
import com.coldchain.modules.identity.internal.domain.model.RefreshToken;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.repository.OrganizationRepository;
import com.coldchain.modules.identity.internal.domain.repository.RefreshTokenRepository;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.domain.service.AccessTokenIssuer;
import com.coldchain.modules.identity.internal.domain.service.IssuedSecret;
import com.coldchain.modules.identity.internal.domain.service.OpaqueTokenFactory;
import com.coldchain.modules.identity.internal.domain.service.SecretHasher;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class AuthenticateUseCase {

    private final AppUserRepository users;

    private final OrganizationRepository organizations;

    private final RoleRepository roles;

    private final RefreshTokenRepository refreshTokens;

    private final SecretHasher hasher;

    private final OpaqueTokenFactory tokens;

    private final AccessTokenIssuer accessTokens;

    private final Clock clock;

    public AuthenticateUseCase(AppUserRepository users, OrganizationRepository organizations,
            RoleRepository roles, RefreshTokenRepository refreshTokens, SecretHasher hasher,
            OpaqueTokenFactory tokens, AccessTokenIssuer accessTokens, Clock clock) {
        this.users = users;
        this.organizations = organizations;
        this.roles = roles;
        this.refreshTokens = refreshTokens;
        this.hasher = hasher;
        this.tokens = tokens;
        this.accessTokens = accessTokens;
        this.clock = clock;
    }

    @Transactional
    public TokenResult execute(AuthenticateCommand command) {
        Optional<AppUser> candidate = users.findByEmail(command.email());
        AppUser user = candidate
                .filter(found -> hasher.matches(command.password(), found.passwordHash()))
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.INVALID_CREDENTIALS));
        if (!user.active()) {
            throw DomainException.of(IdentityErrorCode.USER_NOT_ACTIVE);
        }
        Organization organization = organizations.findById(user.organizationId())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.ORGANIZATION_NOT_FOUND));
        if (!organization.active()) {
            throw DomainException.of(IdentityErrorCode.ORGANIZATION_SUSPENDED);
        }
        Instant now = clock.instant();
        users.save(user.recordLogin(now));
        Set<Scope> scopes = roles.findScopesOf(user.id());
        IssuedSecret refresh = tokens.issue();
        refreshTokens.save(RefreshToken.openFamily(organization.id(), user.id(), refresh.fingerprint(), now,
                accessTokens.refreshTokenLifetime()));
        String accessToken = accessTokens.issue(ActorType.USER, user.id(), organization.id(), scopes);
        return new TokenResult(accessToken, refresh.plainToken(), "Bearer",
                accessTokens.accessTokenLifetime().toSeconds());
    }
}
