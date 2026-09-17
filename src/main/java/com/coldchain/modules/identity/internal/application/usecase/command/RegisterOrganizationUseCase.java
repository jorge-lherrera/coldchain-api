package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.model.Organization;
import com.coldchain.modules.identity.internal.domain.model.Role;
import com.coldchain.modules.identity.internal.domain.model.RoleGrant;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.repository.OrganizationRepository;
import com.coldchain.modules.identity.internal.domain.repository.RoleRepository;
import com.coldchain.modules.identity.internal.domain.service.SecretHasher;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RegisterOrganizationUseCase {

    private final OrganizationRepository organizations;

    private final AppUserRepository users;

    private final RoleRepository roles;

    private final SecretHasher hasher;

    private final Clock clock;

    public RegisterOrganizationUseCase(OrganizationRepository organizations, AppUserRepository users,
            RoleRepository roles, SecretHasher hasher, Clock clock) {
        this.organizations = organizations;
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Transactional
    public RegisterOrganizationResult execute(RegisterOrganizationCommand command) {
        if (organizations.findByTaxId(command.taxId()).isPresent()) {
            throw DomainException.of(IdentityErrorCode.TAX_ID_ALREADY_REGISTERED);
        }
        if (users.findByEmail(command.administratorEmail()).isPresent()) {
            throw DomainException.of(IdentityErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        Organization organization = organizations.save(Organization.createNew(command.taxId(),
                command.legalName(), command.tradeName(), command.kind(), command.country()));
        AppUser administrator = users.save(AppUser.createAdministrator(organization.id(),
                command.administratorEmail(), command.administratorFullName(),
                hasher.hash(command.administratorPassword())));
        Role administratorRole = roles.findByCode(RoleCode.ORG_ADMIN)
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.ROLE_NOT_FOUND));
        roles.grant(RoleGrant.of(administrator.id(), administratorRole.id(), null, clock.instant()));
        return new RegisterOrganizationResult(organization.id(), administrator.id(), administrator.email());
    }
}
