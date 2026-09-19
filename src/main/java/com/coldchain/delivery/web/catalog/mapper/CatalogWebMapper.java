package com.coldchain.delivery.web.catalog.mapper;

import com.coldchain.delivery.web.catalog.dto.CreateProductRequest;
import com.coldchain.delivery.web.catalog.dto.CreateSiteRequest;
import com.coldchain.delivery.web.catalog.dto.CreateStorageProfileRequest;
import com.coldchain.delivery.web.catalog.dto.ProductResponse;
import com.coldchain.delivery.web.catalog.dto.SiteResponse;
import com.coldchain.delivery.web.catalog.dto.StorageProfileResponse;
import com.coldchain.delivery.web.catalog.dto.ThresholdsPayload;
import com.coldchain.delivery.web.catalog.dto.UpdateProductRequest;
import com.coldchain.delivery.web.catalog.dto.UpdateSiteRequest;
import com.coldchain.delivery.web.catalog.dto.UpdateStorageProfileRequest;
import com.coldchain.modules.catalog.api.dto.CreateProductCommand;
import com.coldchain.modules.catalog.api.dto.CreateSiteCommand;
import com.coldchain.modules.catalog.api.dto.CreateStorageProfileCommand;
import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.api.dto.SiteResult;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.api.dto.StorageThresholds;
import com.coldchain.modules.catalog.api.dto.UpdateProductCommand;
import com.coldchain.modules.catalog.api.dto.UpdateSiteCommand;
import com.coldchain.modules.catalog.api.dto.UpdateStorageProfileCommand;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CatalogWebMapper {

    public CreateStorageProfileCommand toCommand(UUID organizationId,
            CreateStorageProfileRequest request) {
        return new CreateStorageProfileCommand(organizationId, request.code(), request.name(),
                toView(request.thresholds()));
    }

    public UpdateStorageProfileCommand toCommand(UUID profileId, UpdateStorageProfileRequest request) {
        return new UpdateStorageProfileCommand(profileId, request.name(), toView(request.thresholds()));
    }

    public CreateProductCommand toCommand(UUID organizationId, CreateProductRequest request) {
        return new CreateProductCommand(organizationId, request.sku(), request.name(),
                request.storageProfileId());
    }

    public UpdateProductCommand toCommand(UUID productId, UpdateProductRequest request) {
        return new UpdateProductCommand(productId, request.name(), request.storageProfileId());
    }

    public CreateSiteCommand toCommand(UUID organizationId, CreateSiteRequest request) {
        return new CreateSiteCommand(organizationId, request.code(), request.name(), request.kind(),
                request.latitude(), request.longitude(), request.timeZone());
    }

    public UpdateSiteCommand toCommand(UUID siteId, UpdateSiteRequest request) {
        return new UpdateSiteCommand(siteId, request.name(), request.latitude(), request.longitude(),
                request.timeZone());
    }

    public StorageProfileResponse toResponse(StorageProfileResult result) {
        return new StorageProfileResponse(result.id(), result.code(), result.name(), result.version(),
                result.status(), toPayload(result.thresholds()));
    }

    public ProductResponse toResponse(ProductResult result) {
        return new ProductResponse(result.id(), result.sku(), result.name(), result.storageProfileId());
    }

    public SiteResponse toResponse(SiteResult result) {
        return new SiteResponse(result.id(), result.code(), result.name(), result.kind(),
                result.latitude(), result.longitude(), result.timeZone());
    }

    private StorageThresholds toView(ThresholdsPayload payload) {
        return new StorageThresholds(payload.minCelsius(), payload.maxCelsius(),
                payload.maxSingleExcursionMinutes(), payload.maxCumulativeExcursionMinutes(),
                payload.minCoveragePercent());
    }

    private ThresholdsPayload toPayload(StorageThresholds view) {
        return new ThresholdsPayload(view.minCelsius(), view.maxCelsius(),
                view.maxSingleExcursionMinutes(), view.maxCumulativeExcursionMinutes(),
                view.minCoveragePercent());
    }
}
