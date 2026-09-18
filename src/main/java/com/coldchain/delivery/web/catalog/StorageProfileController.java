package com.coldchain.delivery.web.catalog;

import com.coldchain.delivery.web.catalog.dto.CreateStorageProfileRequest;
import com.coldchain.delivery.web.catalog.dto.StorageProfileResponse;
import com.coldchain.delivery.web.catalog.dto.UpdateStorageProfileRequest;
import com.coldchain.modules.catalog.api.CatalogApi;
import com.coldchain.shared.paging.SortCatalog;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ResponseFactory;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Storage profiles", description = "The temperature range and tolerances a shipment freezes")
@RequestMapping("/v1/storage-profiles")
public class StorageProfileController {

    private static final SortCatalog<StorageProfileSortField> SORTABLE =
            SortCatalog.of(StorageProfileSortField.class, StorageProfileSortField.CODE,
                    Sort.Direction.ASC);

    private final CatalogApi catalog;

    private final CatalogWebMapper mapper;

    private final ResponseFactory responses;

    private final CurrentActor currentActor;

    public StorageProfileController(CatalogApi catalog, CatalogWebMapper mapper,
            ResponseFactory responses, CurrentActor currentActor) {
        this.catalog = catalog;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping
    @Operation(summary = "Create a storage profile",
            description = "It is born in DRAFT: until it is activated no product may use it.")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<StorageProfileResponse>> create(
            @Valid @RequestBody CreateStorageProfileRequest request) {
        return responses.respond(CatalogSuccessCode.PROFILE_CREATED,
                mapper.toResponse(catalog.createStorageProfile(
                        mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }

    @PutMapping("/{profileId}")
    @Operation(summary = "Edit a storage profile that is still a draft",
            description = "An active profile refuses to change: its thresholds are already frozen into "
                    + "shipments, so the only way forward is a new version.")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<StorageProfileResponse>> update(@PathVariable UUID profileId,
            @Valid @RequestBody UpdateStorageProfileRequest request) {
        return responses.respond(CatalogSuccessCode.PROFILE_UPDATED,
                mapper.toResponse(catalog.updateStorageProfile(mapper.toCommand(profileId, request))));
    }

    @PostMapping("/{profileId}/activation")
    @Operation(summary = "Activate a storage profile",
            description = "From here on a product may point at it and a shipment may freeze it.")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<StorageProfileResponse>> activate(@PathVariable UUID profileId) {
        return responses.respond(CatalogSuccessCode.PROFILE_ACTIVATED,
                mapper.toResponse(catalog.activateStorageProfile(profileId)));
    }

    @PostMapping("/{profileId}/versions")
    @Operation(summary = "Clone a storage profile into its next version",
            description = "The new version is born in DRAFT and the previous one is retired. This is "
                    + "the only way to change thresholds that shipments already copied.")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<StorageProfileResponse>> clone(@PathVariable UUID profileId) {
        return responses.respond(CatalogSuccessCode.PROFILE_CLONED,
                mapper.toResponse(catalog.cloneStorageProfileToNextVersion(profileId)));
    }

    @GetMapping
    @Operation(summary = "List the storage profiles of the organization")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_READ')")
    public ResponseEntity<ApiResponse<List<StorageProfileResponse>>> list(Pageable pageable) {
        return responses.paginated(CatalogSuccessCode.PROFILE_LISTED,
                catalog.listStorageProfiles(currentActor.requireOrganizationId(),
                        SORTABLE.apply(pageable)).map(mapper::toResponse));
    }
}
