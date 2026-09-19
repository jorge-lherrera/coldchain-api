package com.coldchain.modules.catalog.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.api.dto.UpdateProductCommand;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.model.Product;
import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.modules.catalog.internal.domain.repository.ProductRepository;
import com.coldchain.modules.catalog.internal.domain.repository.StorageProfileRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class UpdateProductUseCase {

    private final ProductRepository products;

    private final StorageProfileRepository profiles;

    private final CatalogApiMapper mapper;

    public UpdateProductUseCase(ProductRepository products, StorageProfileRepository profiles,
            CatalogApiMapper mapper) {
        this.products = products;
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @Transactional
    public ProductResult execute(UpdateProductCommand command) {
        Product product = products.findById(command.productId())
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PRODUCT_NOT_FOUND));
        StorageProfile profile = profiles.findById(command.storageProfileId())
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PROFILE_NOT_FOUND));
        if (!profile.usable()) {
            throw DomainException.of(CatalogErrorCode.PROFILE_NOT_USABLE);
        }
        return mapper.toResult(products.save(product.describedAs(command.name(), profile.id())));
    }
}
