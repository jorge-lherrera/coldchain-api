package com.coldchain.modules.identity.internal.application.usecase.query;

import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.api.dto.EffectiveScopesResult;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import java.util.Set;
import java.util.UUID;

@UseCase
public class GetEffectiveScopesUseCase {

    private final AppUserRepository users;

    private final RoleRepository roles;

    public GetEffectiveScopesUseCase(AppUserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    public EffectiveScopesResult execute(UUID userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.USER_NOT_FOUND));
        Set<Scope> scopes = roles.findScopesOf(user.id());
        return new EffectiveScopesResult(user.id(), user.organizationId(), scopes);
    }
}
