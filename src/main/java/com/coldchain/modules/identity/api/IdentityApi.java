package com.coldchain.modules.identity.api;

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
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IdentityApi {

    RegisterOrganizationResult registerOrganization(RegisterOrganizationCommand command);

    InviteUserResult inviteUser(InviteUserCommand command);

    ActivateUserResult activateUser(ActivateUserCommand command);

    TokenResult authenticate(AuthenticateCommand command);

    TokenResult refreshAccess(RefreshAccessCommand command);

    TokenResult issueClientToken(IssueClientTokenCommand command);

    RoleGrantResult assignRole(AssignRoleCommand command);

    void revokeRole(RevokeRoleCommand command);

    ApiClientResult createApiClient(CreateApiClientCommand command);

    EffectiveScopesResult effectiveScopesOf(UUID userId);

    Page<UserResult> listUsers(UUID organizationId, Pageable pageable);
}
