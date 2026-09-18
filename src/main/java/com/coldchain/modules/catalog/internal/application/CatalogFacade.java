package com.coldchain.modules.catalog.internal.application;

import com.coldchain.modules.catalog.api.CatalogApi;
import com.coldchain.modules.catalog.api.dto.CreateProductCommand;
import com.coldchain.modules.catalog.api.dto.CreateSiteCommand;
import com.coldchain.modules.catalog.api.dto.CreateStorageProfileCommand;
import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.api.dto.SiteResult;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.api.dto.UpdateProductCommand;
import com.coldchain.modules.catalog.api.dto.UpdateSiteCommand;
import com.coldchain.modules.catalog.api.dto.UpdateStorageProfileCommand;
import com.coldchain.modules.catalog.internal.application.usecase.command.ActivateStorageProfileUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.command.CloneStorageProfileUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.command.CreateProductUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.command.CreateSiteUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.command.CreateStorageProfileUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.command.RetireProductUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.command.UpdateProductUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.command.UpdateSiteUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.command.UpdateStorageProfileUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.query.GetProductProfileUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.query.GetSiteUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.query.ListProductsUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.query.ListSitesUseCase;
import com.coldchain.modules.catalog.internal.application.usecase.query.ListStorageProfilesUseCase;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CatalogFacade implements CatalogApi {

    private final CreateStorageProfileUseCase createProfile;

    private final UpdateStorageProfileUseCase updateProfile;

    private final ActivateStorageProfileUseCase activateProfile;

    private final CloneStorageProfileUseCase cloneProfile;

    private final ListStorageProfilesUseCase listProfiles;

    private final GetProductProfileUseCase productProfile;

    private final CreateProductUseCase createProduct;

    private final UpdateProductUseCase updateProduct;

    private final RetireProductUseCase retireProduct;

    private final ListProductsUseCase listProducts;

    private final CreateSiteUseCase createSite;

    private final UpdateSiteUseCase updateSite;

    private final ListSitesUseCase listSites;

    private final GetSiteUseCase getSite;

    public CatalogFacade(CreateStorageProfileUseCase createProfile,
            UpdateStorageProfileUseCase updateProfile, ActivateStorageProfileUseCase activateProfile,
            CloneStorageProfileUseCase cloneProfile, ListStorageProfilesUseCase listProfiles,
            GetProductProfileUseCase productProfile, CreateProductUseCase createProduct,
            UpdateProductUseCase updateProduct, RetireProductUseCase retireProduct,
            ListProductsUseCase listProducts, CreateSiteUseCase createSite, UpdateSiteUseCase updateSite,
            ListSitesUseCase listSites, GetSiteUseCase getSite) {
        this.createProfile = createProfile;
        this.updateProfile = updateProfile;
        this.activateProfile = activateProfile;
        this.cloneProfile = cloneProfile;
        this.listProfiles = listProfiles;
        this.productProfile = productProfile;
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.retireProduct = retireProduct;
        this.listProducts = listProducts;
        this.createSite = createSite;
        this.updateSite = updateSite;
        this.listSites = listSites;
        this.getSite = getSite;
    }

    @Override
    public StorageProfileResult createStorageProfile(CreateStorageProfileCommand command) {
        return createProfile.execute(command);
    }

    @Override
    public StorageProfileResult updateStorageProfile(UpdateStorageProfileCommand command) {
        return updateProfile.execute(command);
    }

    @Override
    public StorageProfileResult activateStorageProfile(UUID profileId) {
        return activateProfile.execute(profileId);
    }

    @Override
    public StorageProfileResult cloneStorageProfileToNextVersion(UUID profileId) {
        return cloneProfile.execute(profileId);
    }

    @Override
    public PagedResult<StorageProfileResult> listStorageProfiles(UUID organizationId,
            PageCriteria criteria) {
        return listProfiles.execute(organizationId, criteria);
    }

    @Override
    public StorageProfileResult activeProfileOf(UUID productId) {
        return productProfile.execute(productId);
    }

    @Override
    public ProductResult createProduct(CreateProductCommand command) {
        return createProduct.execute(command);
    }

    @Override
    public ProductResult updateProduct(UpdateProductCommand command) {
        return updateProduct.execute(command);
    }

    @Override
    public void retireProduct(UUID productId) {
        retireProduct.execute(productId);
    }

    @Override
    public PagedResult<ProductResult> listProducts(UUID organizationId, PageCriteria criteria) {
        return listProducts.execute(organizationId, criteria);
    }

    @Override
    public SiteResult createSite(CreateSiteCommand command) {
        return createSite.execute(command);
    }

    @Override
    public SiteResult updateSite(UpdateSiteCommand command) {
        return updateSite.execute(command);
    }

    @Override
    public PagedResult<SiteResult> listSites(UUID organizationId, PageCriteria criteria) {
        return listSites.execute(organizationId, criteria);
    }

    @Override
    public SiteResult siteOf(UUID siteId) {
        return getSite.execute(siteId);
    }
}
