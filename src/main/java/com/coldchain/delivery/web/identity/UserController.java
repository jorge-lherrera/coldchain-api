package com.coldchain.delivery.web.identity;

import com.coldchain.delivery.web.identity.dto.AssignRoleRequest;
import com.coldchain.delivery.web.identity.dto.InviteUserRequest;
import com.coldchain.delivery.web.identity.dto.InviteUserResponse;
import com.coldchain.delivery.web.identity.dto.RoleGrantResponse;
import com.coldchain.delivery.web.identity.dto.UserResponse;
import com.coldchain.delivery.web.identity.mapper.IdentityWebMapper;
import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.dto.AssignRoleCommand;
import com.coldchain.modules.identity.api.dto.RevokeRoleCommand;
import com.coldchain.shared.pagination.SortCatalog;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ApiResponseFactory;
import com.coldchain.shared.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Users", description = "The people of an organization and the roles they hold")
@RequestMapping("/v1/users")
public class UserController {

    private static final SortCatalog<UserSortField> SORTABLE =
            SortCatalog.of(UserSortField.class, UserSortField.FULL_NAME, Sort.Direction.ASC);

    private final IdentityApi identity;

    private final IdentityWebMapper mapper;

    private final ApiResponseFactory responses;

    private final CurrentActor currentActor;

    public UserController(IdentityApi identity, IdentityWebMapper mapper, ApiResponseFactory responses,
            CurrentActor currentActor) {
        this.identity = identity;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping
    @Operation(summary = "Invite a user",
            description = "Creates the user as INVITED and returns the activation token to hand over.")
    @PreAuthorize("hasAuthority('SCOPE_USER_WRITE')")
    public ResponseEntity<ApiResponse<InviteUserResponse>> invite(
            @Valid @RequestBody InviteUserRequest request) {
        return responses.respond(IdentitySuccessCode.USER_INVITED, mapper.toResponse(
                identity.inviteUser(mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }

    @GetMapping
    @Operation(summary = "List the users of the caller's organization",
            description = "Always paginated, and always scoped to the organization in the token.")
    @PreAuthorize("hasAuthority('SCOPE_USER_READ')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> list(Pageable pageable) {
        return responses.paginated(IdentitySuccessCode.USER_LISTED,
                identity.listUsers(currentActor.requireOrganizationId(), SORTABLE.apply(pageable))
                        .map(mapper::toResponse));
    }

    @PostMapping("/{userId}/roles")
    @Operation(summary = "Grant a role to a user")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_WRITE')")
    public ResponseEntity<ApiResponse<RoleGrantResponse>> assignRole(@PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request) {
        return responses.respond(IdentitySuccessCode.ROLE_ASSIGNED,
                mapper.toResponse(identity.assignRole(new AssignRoleCommand(userId, request.role()))));
    }

    @DeleteMapping("/{userId}/roles/{role}")
    @Operation(summary = "Revoke a role from a user",
            description = "Refuses to remove the last administrator of an organization.")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_WRITE')")
    public ResponseEntity<ApiResponse<Void>> revokeRole(@PathVariable UUID userId,
            @PathVariable RoleCode role) {
        identity.revokeRole(new RevokeRoleCommand(userId, role));
        return responses.respond(IdentitySuccessCode.ROLE_REVOKED, null);
    }
}
