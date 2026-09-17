package com.coldchain.delivery.web.identity;

import com.coldchain.delivery.web.identity.dto.ActivateUserRequest;
import com.coldchain.delivery.web.identity.dto.ActivateUserResponse;
import com.coldchain.delivery.web.identity.dto.ClientTokenRequest;
import com.coldchain.delivery.web.identity.dto.EffectiveScopesResponse;
import com.coldchain.delivery.web.identity.dto.LoginRequest;
import com.coldchain.delivery.web.identity.dto.RefreshRequest;
import com.coldchain.delivery.web.identity.dto.TokenResponse;
import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ResponseFactory;
import com.coldchain.shared.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication", description = "Tokens, activation and the scopes a caller holds")
@RequestMapping("/v1/auth")
public class AuthenticationController {

    private final IdentityApi identity;

    private final IdentityWebMapper mapper;

    private final ResponseFactory responses;

    private final CurrentActor currentActor;

    public AuthenticationController(IdentityApi identity, IdentityWebMapper mapper,
            ResponseFactory responses, CurrentActor currentActor) {
        this.identity = identity;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange an email and a password for tokens",
            description = "Answers the same way whether the email is unknown or the password is "
                    + "wrong, so the endpoint cannot be used to find out who has an account.",
            security = {})
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return responses.respond(IdentitySuccessCode.TOKEN_ISSUED,
                mapper.toResponse(identity.authenticate(mapper.toCommand(request))));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token",
            description = "The presented token is spent and a new one is issued. Presenting a spent "
                    + "token revokes its whole family: reuse is treated as a leak.", security = {})
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return responses.respond(IdentitySuccessCode.TOKEN_ISSUED,
                mapper.toResponse(identity.refreshAccess(mapper.toCommand(request))));
    }

    @PostMapping("/client-token")
    @Operation(summary = "Exchange machine credentials for an access token",
            description = "The organization travels signed in the token; a client never chooses it.",
            security = {})
    public ResponseEntity<ApiResponse<TokenResponse>> clientToken(
            @Valid @RequestBody ClientTokenRequest request) {
        return responses.respond(IdentitySuccessCode.TOKEN_ISSUED,
                mapper.toResponse(identity.issueClientToken(mapper.toCommand(request))));
    }

    @PostMapping("/activation")
    @Operation(summary = "Activate an invited user",
            description = "Consumes the single-use activation token and sets the first password.",
            security = {})
    public ResponseEntity<ApiResponse<ActivateUserResponse>> activate(
            @Valid @RequestBody ActivateUserRequest request) {
        return responses.respond(IdentitySuccessCode.USER_ACTIVATED,
                mapper.toResponse(identity.activateUser(mapper.toCommand(request))));
    }

    @GetMapping("/scopes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "The effective scopes of the caller",
            description = "The union of the scopes granted by every role the caller holds.")
    public ResponseEntity<ApiResponse<EffectiveScopesResponse>> myScopes() {
        return responses.respond(IdentitySuccessCode.SCOPES_RETRIEVED,
                mapper.toResponse(identity.effectiveScopesOf(currentActor.requireId())));
    }
}
