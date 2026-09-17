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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return responses.respond(IdentitySuccessCode.TOKEN_ISSUED,
                mapper.toResponse(identity.authenticate(mapper.toCommand(request))));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return responses.respond(IdentitySuccessCode.TOKEN_ISSUED,
                mapper.toResponse(identity.refreshAccess(mapper.toCommand(request))));
    }

    @PostMapping("/client-token")
    public ResponseEntity<ApiResponse<TokenResponse>> clientToken(
            @Valid @RequestBody ClientTokenRequest request) {
        return responses.respond(IdentitySuccessCode.TOKEN_ISSUED,
                mapper.toResponse(identity.issueClientToken(mapper.toCommand(request))));
    }

    @PostMapping("/activation")
    public ResponseEntity<ApiResponse<ActivateUserResponse>> activate(
            @Valid @RequestBody ActivateUserRequest request) {
        return responses.respond(IdentitySuccessCode.USER_ACTIVATED,
                mapper.toResponse(identity.activateUser(mapper.toCommand(request))));
    }

    @GetMapping("/scopes")
    public ResponseEntity<ApiResponse<EffectiveScopesResponse>> myScopes() {
        return responses.respond(IdentitySuccessCode.SCOPES_RETRIEVED,
                mapper.toResponse(identity.effectiveScopesOf(currentActor.requireId())));
    }
}
