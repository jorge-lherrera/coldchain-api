package com.coldchain.delivery.web.identity;

import com.coldchain.delivery.web.identity.dto.ApiClientResponse;
import com.coldchain.delivery.web.identity.dto.CreateApiClientRequest;
import com.coldchain.delivery.web.identity.dto.RegisterOrganizationRequest;
import com.coldchain.delivery.web.identity.dto.RegisterOrganizationResponse;
import com.coldchain.delivery.web.identity.mapper.IdentityWebMapper;
import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ApiResponseFactory;
import com.coldchain.shared.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Organizations", description = "Tenants and the machine credentials they own")
@RequestMapping("/v1/organizations")
public class OrganizationController {

    private final IdentityApi identity;

    private final IdentityWebMapper mapper;

    private final ApiResponseFactory responses;

    private final CurrentActor currentActor;

    public OrganizationController(IdentityApi identity, IdentityWebMapper mapper, ApiResponseFactory responses,
            CurrentActor currentActor) {
        this.identity = identity;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping
    @Operation(summary = "Register an organization and its first administrator",
            description = "The only write that needs no credentials. It creates the tenant, its first "
                    + "ORG_ADMIN and the password that administrator logs in with.", security = {})
    public ResponseEntity<ApiResponse<RegisterOrganizationResponse>> register(
            @Valid @RequestBody RegisterOrganizationRequest request) {
        return responses.respond(IdentitySuccessCode.ORGANIZATION_REGISTERED,
                mapper.toResponse(identity.registerOrganization(mapper.toCommand(request))));
    }

    @PostMapping("/api-clients")
    @Operation(summary = "Create a machine credential",
            description = "Returns the client secret once and never again: only its hash is stored.")
    @PreAuthorize("hasAuthority('SCOPE_ORGANIZATION_WRITE')")
    public ResponseEntity<ApiResponse<ApiClientResponse>> createApiClient(
            @Valid @RequestBody CreateApiClientRequest request) {
        return responses.respond(IdentitySuccessCode.API_CLIENT_CREATED,
                mapper.toResponse(identity.createApiClient(
                        mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }
}
