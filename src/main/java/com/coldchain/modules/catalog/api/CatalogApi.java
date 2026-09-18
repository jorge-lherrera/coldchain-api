package com.coldchain.modules.catalog.api;

import com.coldchain.modules.catalog.api.dto.CreateProductCommand;
import com.coldchain.modules.catalog.api.dto.CreateSiteCommand;
import com.coldchain.modules.catalog.api.dto.CreateStorageProfileCommand;
import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.api.dto.SiteResult;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.api.dto.UpdateProductCommand;
import com.coldchain.modules.catalog.api.dto.UpdateSiteCommand;
import com.coldchain.modules.catalog.api.dto.UpdateStorageProfileCommand;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;

public interface CatalogApi {

    StorageProfileResult createStorageProfile(CreateStorageProfileCommand command);

    StorageProfileResult updateStorageProfile(UpdateStorageProfileCommand command);

    StorageProfileResult activateStorageProfile(UUID profileId);

    StorageProfileResult cloneStorageProfileToNextVersion(UUID profileId);

    PagedResult<StorageProfileResult> listStorageProfiles(UUID organizationId, PageCriteria criteria);

    StorageProfileResult activeProfileOf(UUID productId);

    ProductResult createProduct(CreateProductCommand command);

    ProductResult updateProduct(UpdateProductCommand command);

    ProductResult productOf(UUID productId);

    void retireProduct(UUID productId);

    PagedResult<ProductResult> listProducts(UUID organizationId, PageCriteria criteria);

    SiteResult createSite(CreateSiteCommand command);

    SiteResult updateSite(UpdateSiteCommand command);

    PagedResult<SiteResult> listSites(UUID organizationId, PageCriteria criteria);

    SiteResult siteOf(UUID siteId);
}
