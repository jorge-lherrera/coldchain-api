package com.coldchain.delivery.web.identity.mapper;

import com.coldchain.delivery.web.identity.dto.ActivateUserRequest;
import com.coldchain.delivery.web.identity.dto.ActivateUserResponse;
import com.coldchain.delivery.web.identity.dto.ApiClientResponse;
import com.coldchain.delivery.web.identity.dto.ClientTokenRequest;
import com.coldchain.delivery.web.identity.dto.CreateApiClientRequest;
import com.coldchain.delivery.web.identity.dto.EffectiveScopesResponse;
import com.coldchain.delivery.web.identity.dto.InviteUserRequest;
import com.coldchain.delivery.web.identity.dto.InviteUserResponse;
import com.coldchain.delivery.web.identity.dto.LoginRequest;
import com.coldchain.delivery.web.identity.dto.RefreshRequest;
import com.coldchain.delivery.web.identity.dto.RegisterOrganizationRequest;
import com.coldchain.delivery.web.identity.dto.RegisterOrganizationResponse;
import com.coldchain.delivery.web.identity.dto.RoleGrantResponse;
import com.coldchain.delivery.web.identity.dto.TokenResponse;
import com.coldchain.delivery.web.identity.dto.UserResponse;
import com.coldchain.modules.identity.api.dto.ActivateUserCommand;
import com.coldchain.modules.identity.api.dto.ActivateUserResult;
import com.coldchain.modules.identity.api.dto.ApiClientResult;
import com.coldchain.modules.identity.api.dto.AuthenticateCommand;
import com.coldchain.modules.identity.api.dto.CreateApiClientCommand;
import com.coldchain.modules.identity.api.dto.EffectiveScopesResult;
import com.coldchain.modules.identity.api.dto.InviteUserCommand;
import com.coldchain.modules.identity.api.dto.InviteUserResult;
import com.coldchain.modules.identity.api.dto.IssueClientTokenCommand;
import com.coldchain.modules.identity.api.dto.RefreshAccessCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.identity.api.dto.RoleGrantResult;
import com.coldchain.modules.identity.api.dto.TokenResult;
import com.coldchain.modules.identity.api.dto.UserResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdentityWebMapper {

    public RegisterOrganizationCommand toCommand(RegisterOrganizationRequest request) {
        return new RegisterOrganizationCommand(request.taxId(), request.legalName(), request.tradeName(),
                request.kind(), request.country(), request.administratorEmail(),
                request.administratorFullName(), request.administratorPassword());
    }

    public RegisterOrganizationResponse toResponse(RegisterOrganizationResult result) {
        return new RegisterOrganizationResponse(result.organizationId(), result.administratorId(),
                result.email());
    }

    public InviteUserCommand toCommand(UUID organizationId, InviteUserRequest request) {
        return new InviteUserCommand(organizationId, request.email(), request.fullName(), request.role());
    }

    public InviteUserResponse toResponse(InviteUserResult result) {
        return new InviteUserResponse(result.userId(), result.email(), result.activationToken(),
                result.expiresAt());
    }

    public ActivateUserCommand toCommand(ActivateUserRequest request) {
        return new ActivateUserCommand(request.activationToken(), request.password());
    }

    public ActivateUserResponse toResponse(ActivateUserResult result) {
        return new ActivateUserResponse(result.userId(), result.email());
    }

    public AuthenticateCommand toCommand(LoginRequest request) {
        return new AuthenticateCommand(request.email(), request.password());
    }

    public RefreshAccessCommand toCommand(RefreshRequest request) {
        return new RefreshAccessCommand(request.refreshToken());
    }

    public IssueClientTokenCommand toCommand(ClientTokenRequest request) {
        return new IssueClientTokenCommand(request.clientId(), request.clientSecret());
    }

    public TokenResponse toResponse(TokenResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken(), result.tokenType(),
                result.expiresInSeconds());
    }

    public RoleGrantResponse toResponse(RoleGrantResult result) {
        return new RoleGrantResponse(result.userId(), result.role(), result.grantedBy(), result.grantedAt());
    }

    public CreateApiClientCommand toCommand(UUID organizationId, CreateApiClientRequest request) {
        return new CreateApiClientCommand(organizationId, request.label(), request.scopes());
    }

    public ApiClientResponse toResponse(ApiClientResult result) {
        return new ApiClientResponse(result.id(), result.clientId(), result.clientSecret(), result.label(),
                result.status(), result.scopes());
    }

    public UserResponse toResponse(UserResult result) {
        return new UserResponse(result.id(), result.organizationId(), result.email(), result.fullName(),
                result.status(), result.lastLoginAt());
    }

    public EffectiveScopesResponse toResponse(EffectiveScopesResult result) {
        return new EffectiveScopesResponse(result.userId(), result.organizationId(), result.scopes());
    }
}
