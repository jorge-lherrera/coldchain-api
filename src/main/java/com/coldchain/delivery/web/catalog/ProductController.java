package com.coldchain.delivery.web.catalog;

import com.coldchain.delivery.web.catalog.dto.CreateProductRequest;
import com.coldchain.delivery.web.catalog.dto.ProductResponse;
import com.coldchain.delivery.web.catalog.dto.UpdateProductRequest;
import com.coldchain.delivery.web.catalog.mapper.CatalogWebMapper;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Products", description = "What travels, and the storage profile it must be kept under")
@RequestMapping("/v1/products")
public class ProductController {

    private static final SortCatalog<ProductSortField> SORTABLE =
            SortCatalog.of(ProductSortField.class, ProductSortField.SKU, Sort.Direction.ASC);

    private final CatalogApi catalog;

    private final CatalogWebMapper mapper;

    private final ApiResponseFactory responses;

    private final CurrentActor currentActor;

    public ProductController(CatalogApi catalog, CatalogWebMapper mapper, ApiResponseFactory responses,
            CurrentActor currentActor) {
        this.catalog = catalog;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping
    @Operation(summary = "Create a product",
            description = "It must point at an active storage profile: a draft is not a promise yet.")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {
        return responses.respond(CatalogSuccessCode.PRODUCT_CREATED,
                mapper.toResponse(catalog.createProduct(
                        mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update a product")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return responses.respond(CatalogSuccessCode.PRODUCT_UPDATED,
                mapper.toResponse(catalog.updateProduct(mapper.toCommand(productId, request))));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Retire a product",
            description = "A soft delete: the row stays for the shipments that reference it, and the "
                    + "SKU becomes free again.")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_WRITE')")
    public ResponseEntity<ApiResponse<Void>> retire(@PathVariable UUID productId) {
        catalog.retireProduct(productId);
        return responses.respond(CatalogSuccessCode.PRODUCT_RETIRED, null);
    }

    @GetMapping
    @Operation(summary = "List the products of the organization")
    @PreAuthorize("hasAuthority('SCOPE_CATALOG_READ')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> list(Pageable pageable) {
        return responses.paginated(CatalogSuccessCode.PRODUCT_LISTED,
                catalog.listProducts(currentActor.requireOrganizationId(), SORTABLE.apply(pageable))
                        .map(mapper::toResponse));
    }
}
