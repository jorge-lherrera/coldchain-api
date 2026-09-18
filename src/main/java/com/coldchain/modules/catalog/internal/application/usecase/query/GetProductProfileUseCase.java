package com.coldchain.modules.catalog.internal.application.usecase.query;

import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.model.Product;
import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.modules.catalog.internal.domain.repository.ProductRepository;
import com.coldchain.modules.catalog.internal.domain.repository.StorageProfileRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.util.UUID;

@UseCase
public class GetProductProfileUseCase {

    private final ProductRepository products;

    private final StorageProfileRepository profiles;

    private final CatalogApiMapper mapper;

    public GetProductProfileUseCase(ProductRepository products, StorageProfileRepository profiles,
            CatalogApiMapper mapper) {
        this.products = products;
        this.profiles = profiles;
        this.mapper = mapper;
    }

    public StorageProfileResult execute(UUID productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PRODUCT_NOT_FOUND));
        StorageProfile profile = profiles.findById(product.storageProfileId())
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PROFILE_NOT_FOUND));
        return mapper.toResult(profile);
    }
}
