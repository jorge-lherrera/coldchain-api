package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.dto.AssignRoleCommand;
import com.coldchain.modules.identity.api.dto.RoleGrantResult;
import com.coldchain.modules.identity.api.event.RoleAssigned;
import com.coldchain.modules.identity.internal.application.CurrentIdentityActor;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.model.Role;
import com.coldchain.modules.identity.internal.domain.model.RoleGrant;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import com.coldchain.shared.security.CurrentActor;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class AssignRoleUseCase {

    private final AppUserRepository users;

    private final RoleRepository roles;

    private final CurrentActor currentActor;

    private final CurrentIdentityActor identityActor;

    private final ApplicationEventPublisher events;

    private final Clock clock;

    public AssignRoleUseCase(AppUserRepository users, RoleRepository roles, CurrentActor currentActor,
            CurrentIdentityActor identityActor, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.currentActor = currentActor;
        this.identityActor = identityActor;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public RoleGrantResult execute(AssignRoleCommand command) {
        AppUser user = users.findById(command.userId())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.USER_NOT_FOUND));
        Role role = roles.findByCode(command.role())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.ROLE_NOT_FOUND));
        if (roles.granted(user.id(), role.id())) {
            throw DomainException.of(IdentityErrorCode.ROLE_ALREADY_GRANTED);
        }
        RoleGrant grant = roles.grant(RoleGrant.of(user.id(), role.id(), currentActor.id().orElse(null),
                clock.instant()));
        events.publishEvent(new RoleAssigned(user.organizationId(), identityActor.type(),
                identityActor.id(), user.id(), command.role(), grant.grantedAt()));
        return new RoleGrantResult(grant.userId(), command.role(), grant.grantedBy(), grant.grantedAt());
    }
}
