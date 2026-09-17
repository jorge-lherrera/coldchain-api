package com.coldchain.delivery.web.identity;

import com.coldchain.delivery.web.identity.dto.ApiClientResponse;
import com.coldchain.delivery.web.identity.dto.CreateApiClientRequest;
import com.coldchain.delivery.web.identity.dto.RegisterOrganizationRequest;
import com.coldchain.delivery.web.identity.dto.RegisterOrganizationResponse;
import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ResponseFactory;
import com.coldchain.shared.security.CurrentActor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/organizations")
public class OrganizationController {

    private final IdentityApi identity;

    private final IdentityWebMapper mapper;

    private final ResponseFactory responses;

    private final CurrentActor currentActor;

    public OrganizationController(IdentityApi identity, IdentityWebMapper mapper, ResponseFactory responses,
            CurrentActor currentActor) {
        this.identity = identity;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RegisterOrganizationResponse>> register(
            @Valid @RequestBody RegisterOrganizationRequest request) {
        return responses.respond(IdentitySuccessCode.ORGANIZATION_REGISTERED,
                mapper.toResponse(identity.registerOrganization(mapper.toCommand(request))));
    }

    @PostMapping("/api-clients")
    @PreAuthorize("hasAuthority('SCOPE_ORGANIZATION_WRITE')")
    public ResponseEntity<ApiResponse<ApiClientResponse>> createApiClient(
            @Valid @RequestBody CreateApiClientRequest request) {
        return responses.respond(IdentitySuccessCode.API_CLIENT_CREATED,
                mapper.toResponse(identity.createApiClient(
                        mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }
}
