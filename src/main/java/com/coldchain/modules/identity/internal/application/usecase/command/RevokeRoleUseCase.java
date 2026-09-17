package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.dto.RevokeRoleCommand;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.model.Role;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RevokeRoleUseCase {

    private final AppUserRepository users;

    private final RoleRepository roles;

    public RevokeRoleUseCase(AppUserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Transactional
    public void execute(RevokeRoleCommand command) {
        AppUser user = users.findById(command.userId())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.USER_NOT_FOUND));
        Role role = roles.findByCode(command.role())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.ROLE_NOT_FOUND));
        if (command.role() == RoleCode.ORG_ADMIN
                && users.countAdministrators(user.organizationId()) <= 1) {
            throw DomainException.of(IdentityErrorCode.LAST_ADMINISTRATOR);
        }
        if (!roles.revoke(user.id(), role.id())) {
            throw DomainException.of(IdentityErrorCode.ROLE_NOT_GRANTED);
        }
    }
}
