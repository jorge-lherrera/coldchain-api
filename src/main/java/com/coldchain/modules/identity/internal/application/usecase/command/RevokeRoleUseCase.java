package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.dto.RevokeRoleCommand;
import com.coldchain.modules.identity.api.event.RoleRevoked;
import com.coldchain.modules.identity.internal.application.CurrentIdentityActor;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.model.Role;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RevokeRoleUseCase {

    private final AppUserRepository users;

    private final RoleRepository roles;

    private final CurrentIdentityActor identityActor;

    private final ApplicationEventPublisher events;

    private final Clock clock;

    public RevokeRoleUseCase(AppUserRepository users, RoleRepository roles,
            CurrentIdentityActor identityActor, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.identityActor = identityActor;
        this.events = events;
        this.clock = clock;
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
        events.publishEvent(new RoleRevoked(user.organizationId(), identityActor.type(),
                identityActor.id(), user.id(), command.role(), clock.instant()));
    }
}
