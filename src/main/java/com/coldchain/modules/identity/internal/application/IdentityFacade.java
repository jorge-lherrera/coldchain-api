package com.coldchain.modules.identity.internal.application;

import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.dto.ActivateUserCommand;
import com.coldchain.modules.identity.api.dto.ActivateUserResult;
import com.coldchain.modules.identity.api.dto.ApiClientResult;
import com.coldchain.modules.identity.api.dto.AssignRoleCommand;
import com.coldchain.modules.identity.api.dto.AuthenticateCommand;
import com.coldchain.modules.identity.api.dto.CreateApiClientCommand;
import com.coldchain.modules.identity.api.dto.EffectiveScopesResult;
import com.coldchain.modules.identity.api.dto.InviteUserCommand;
import com.coldchain.modules.identity.api.dto.InviteUserResult;
import com.coldchain.modules.identity.api.dto.IssueClientTokenCommand;
import com.coldchain.modules.identity.api.dto.RefreshAccessCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.identity.api.dto.RevokeRoleCommand;
import com.coldchain.modules.identity.api.dto.RoleGrantResult;
import com.coldchain.modules.identity.api.dto.TokenResult;
import com.coldchain.modules.identity.api.dto.UserResult;
import com.coldchain.modules.identity.internal.application.usecase.command.ActivateUserUseCase;
import com.coldchain.modules.identity.internal.application.usecase.command.AssignRoleUseCase;
import com.coldchain.modules.identity.internal.application.usecase.command.AuthenticateUseCase;
import com.coldchain.modules.identity.internal.application.usecase.command.CreateApiClientUseCase;
import com.coldchain.modules.identity.internal.application.usecase.command.InviteUserUseCase;
import com.coldchain.modules.identity.internal.application.usecase.command.IssueClientTokenUseCase;
import com.coldchain.modules.identity.internal.application.usecase.command.RefreshAccessUseCase;
import com.coldchain.modules.identity.internal.application.usecase.command.RegisterOrganizationUseCase;
import com.coldchain.modules.identity.internal.application.usecase.command.RevokeRoleUseCase;
import com.coldchain.modules.identity.internal.application.usecase.query.GetEffectiveScopesUseCase;
import com.coldchain.modules.identity.internal.application.usecase.query.ListUsersUseCase;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdentityFacade implements IdentityApi {

    private final RegisterOrganizationUseCase registerOrganization;

    private final InviteUserUseCase inviteUser;

    private final ActivateUserUseCase activateUser;

    private final AuthenticateUseCase authenticate;

    private final RefreshAccessUseCase refreshAccess;

    private final IssueClientTokenUseCase issueClientToken;

    private final AssignRoleUseCase assignRole;

    private final RevokeRoleUseCase revokeRole;

    private final CreateApiClientUseCase createApiClient;

    private final GetEffectiveScopesUseCase getEffectiveScopes;

    private final ListUsersUseCase listUsers;

    public IdentityFacade(RegisterOrganizationUseCase registerOrganization, InviteUserUseCase inviteUser,
            ActivateUserUseCase activateUser, AuthenticateUseCase authenticate,
            RefreshAccessUseCase refreshAccess, IssueClientTokenUseCase issueClientToken,
            AssignRoleUseCase assignRole, RevokeRoleUseCase revokeRole,
            CreateApiClientUseCase createApiClient, GetEffectiveScopesUseCase getEffectiveScopes,
            ListUsersUseCase listUsers) {
        this.registerOrganization = registerOrganization;
        this.inviteUser = inviteUser;
        this.activateUser = activateUser;
        this.authenticate = authenticate;
        this.refreshAccess = refreshAccess;
        this.issueClientToken = issueClientToken;
        this.assignRole = assignRole;
        this.revokeRole = revokeRole;
        this.createApiClient = createApiClient;
        this.getEffectiveScopes = getEffectiveScopes;
        this.listUsers = listUsers;
    }

    @Override
    public RegisterOrganizationResult registerOrganization(RegisterOrganizationCommand command) {
        return registerOrganization.execute(command);
    }

    @Override
    public InviteUserResult inviteUser(InviteUserCommand command) {
        return inviteUser.execute(command);
    }

    @Override
    public ActivateUserResult activateUser(ActivateUserCommand command) {
        return activateUser.execute(command);
    }

    @Override
    public TokenResult authenticate(AuthenticateCommand command) {
        return authenticate.execute(command);
    }

    @Override
    public TokenResult refreshAccess(RefreshAccessCommand command) {
        return refreshAccess.execute(command);
    }

    @Override
    public TokenResult issueClientToken(IssueClientTokenCommand command) {
        return issueClientToken.execute(command);
    }

    @Override
    public RoleGrantResult assignRole(AssignRoleCommand command) {
        return assignRole.execute(command);
    }

    @Override
    public void revokeRole(RevokeRoleCommand command) {
        revokeRole.execute(command);
    }

    @Override
    public ApiClientResult createApiClient(CreateApiClientCommand command) {
        return createApiClient.execute(command);
    }

    @Override
    public EffectiveScopesResult effectiveScopesOf(UUID userId) {
        return getEffectiveScopes.execute(userId);
    }

    @Override
    public PagedResult<UserResult> listUsers(UUID organizationId, PageCriteria criteria) {
        return listUsers.execute(organizationId, criteria);
    }
}
