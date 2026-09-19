package com.coldchain.delivery.web.catalog;

import com.coldchain.delivery.web.catalog.dto.CreateSiteRequest;
import com.coldchain.delivery.web.catalog.dto.SiteResponse;
import com.coldchain.delivery.web.catalog.dto.UpdateSiteRequest;
import com.coldchain.modules.catalog.api.CatalogApi;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Sites", description = "Where a shipment starts, passes through and ends")
@RequestMapping("/v1/sites")
public class SiteController {

    private static final SortCatalog<SiteSortField> SORTABLE =
            SortCatalog.of(SiteSortField.class, SiteSortField.CODE, Sort.Direction.ASC);

    private final CatalogApi catalog;

    private final CatalogWebMapper mapper;

    private final ApiResponseFactory responses;

    private final CurrentActor currentActor;

    public SiteController(CatalogApi catalog, CatalogWebMapper mapper, ApiResponseFactory responses,
            CurrentActor currentActor) {
        this.catalog = catalog;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping
    @Operation(summary = "Create a site",
            description = "The time zone travels as an IANA identifier, because an offset changes "
                    + "twice a year and the identifier does not.")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<SiteResponse>> create(
            @Valid @RequestBody CreateSiteRequest request) {
        return responses.respond(CatalogSuccessCode.SITE_CREATED,
                mapper.toResponse(catalog.createSite(
                        mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }

    @PutMapping("/{siteId}")
    @Operation(summary = "Update a site")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<SiteResponse>> update(@PathVariable UUID siteId,
            @Valid @RequestBody UpdateSiteRequest request) {
        return responses.respond(CatalogSuccessCode.SITE_UPDATED,
                mapper.toResponse(catalog.updateSite(mapper.toCommand(siteId, request))));
    }

    @GetMapping
    @Operation(summary = "List the sites of the organization")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_READ')")
    public ResponseEntity<ApiResponse<List<SiteResponse>>> list(Pageable pageable) {
        return responses.paginated(CatalogSuccessCode.SITE_LISTED,
                catalog.listSites(currentActor.requireOrganizationId(), SORTABLE.apply(pageable))
                        .map(mapper::toResponse));
    }
}
