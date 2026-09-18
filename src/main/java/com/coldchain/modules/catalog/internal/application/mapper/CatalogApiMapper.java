package com.coldchain.modules.catalog.internal.application.mapper;

import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.api.dto.SiteResult;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.api.dto.StorageThresholds;
import com.coldchain.modules.catalog.internal.domain.model.Product;
import com.coldchain.modules.catalog.internal.domain.model.Site;
import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.modules.catalog.internal.domain.model.Thresholds;
import org.springframework.stereotype.Component;

@Component
public class CatalogApiMapper {

    public StorageProfileResult toResult(StorageProfile profile) {
        return new StorageProfileResult(profile.id(), profile.organizationId(), profile.code(),
                profile.name(), profile.version(), profile.status(), toView(profile.thresholds()));
    }

    public ProductResult toResult(Product product) {
        return new ProductResult(product.id(), product.organizationId(), product.sku(), product.name(),
                product.storageProfileId());
    }

    public SiteResult toResult(Site site) {
        return new SiteResult(site.id(), site.organizationId(), site.code(), site.name(), site.kind(),
                site.latitude(), site.longitude(), site.timeZone());
    }

    public StorageThresholds toView(Thresholds thresholds) {
        return new StorageThresholds(thresholds.minCelsius(), thresholds.maxCelsius(),
                thresholds.maxSingleExcursionMinutes(), thresholds.maxCumulativeExcursionMinutes(),
                thresholds.minCoveragePercent());
    }

    public Thresholds toDomain(StorageThresholds view) {
        return new Thresholds(view.minCelsius(), view.maxCelsius(), view.maxSingleExcursionMinutes(),
                view.maxCumulativeExcursionMinutes(), view.minCoveragePercent());
    }
}
