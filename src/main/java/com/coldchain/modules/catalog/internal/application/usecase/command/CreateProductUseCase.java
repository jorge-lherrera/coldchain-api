package com.coldchain.modules.catalog.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.dto.CreateProductCommand;
import com.coldchain.modules.catalog.api.dto.ProductResult;
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
public class CreateProductUseCase {

    private final ProductRepository products;

    private final StorageProfileRepository profiles;

    private final CatalogApiMapper mapper;

    public CreateProductUseCase(ProductRepository products, StorageProfileRepository profiles,
            CatalogApiMapper mapper) {
        this.products = products;
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @Transactional
    public ProductResult execute(CreateProductCommand command) {
        StorageProfile profile = profiles.findById(command.storageProfileId())
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PROFILE_NOT_FOUND));
        if (!profile.usable()) {
            throw DomainException.of(CatalogErrorCode.PROFILE_NOT_USABLE);
        }
        return mapper.toResult(products.save(Product.createNew(command.organizationId(), profile.id(),
                command.sku(), command.name())));
    }
}
