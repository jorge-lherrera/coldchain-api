package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.dto.InviteUserCommand;
import com.coldchain.modules.identity.api.dto.InviteUserResult;
import com.coldchain.modules.identity.api.event.UserInvited;
import com.coldchain.modules.identity.internal.application.CurrentIdentityActor;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.model.Role;
import com.coldchain.modules.identity.internal.domain.model.RoleGrant;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.repository.OrganizationRepository;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.domain.service.IssuedSecret;
import com.coldchain.modules.identity.internal.domain.service.OpaqueTokenFactory;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.security.CurrentActor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class InviteUserUseCase {

    private final AppUserRepository users;

    private final OrganizationRepository organizations;

    private final RoleRepository roles;

    private final OpaqueTokenFactory tokens;

    private final CurrentActor currentActor;

    private final CurrentIdentityActor identityActor;

    private final ApplicationEventPublisher events;

    private final Clock clock;

    public InviteUserUseCase(AppUserRepository users, OrganizationRepository organizations,
            RoleRepository roles, OpaqueTokenFactory tokens, CurrentActor currentActor,
            CurrentIdentityActor identityActor, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.organizations = organizations;
        this.roles = roles;
        this.tokens = tokens;
        this.currentActor = currentActor;
        this.identityActor = identityActor;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public InviteUserResult execute(InviteUserCommand command) {
        organizations.findById(command.organizationId())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.ORGANIZATION_NOT_FOUND));
        if (users.findByEmail(command.email()).isPresent()) {
            throw DomainException.of(IdentityErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        Role role = roles.findByCode(command.role())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.ROLE_NOT_FOUND));
        IssuedSecret activation = tokens.issue();
        Instant expiresAt = clock.instant().plus(tokens.activationLifetime());
        AppUser invited = users.save(AppUser.invite(command.organizationId(), command.email(),
                command.fullName(), activation.fingerprint(), expiresAt));
        roles.grant(RoleGrant.of(invited.id(), role.id(), currentActor.id().orElse(null), clock.instant()));
        events.publishEvent(new UserInvited(command.organizationId(), identityActor.type(),
                identityActor.id(), invited.id(), command.role(), clock.instant()));
        return new InviteUserResult(invited.id(), invited.email(), activation.plainToken(), expiresAt);
    }
}
