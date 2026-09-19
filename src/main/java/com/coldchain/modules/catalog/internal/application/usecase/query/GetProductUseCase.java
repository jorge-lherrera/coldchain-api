package com.coldchain.modules.catalog.internal.application.usecase.query;

import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.repository.ProductRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import java.util.UUID;

@UseCase
public class GetProductUseCase {

    private final ProductRepository products;

    private final CatalogApiMapper mapper;

    public GetProductUseCase(ProductRepository products, CatalogApiMapper mapper) {
        this.products = products;
        this.mapper = mapper;
    }

    public ProductResult execute(UUID productId) {
        return products.findById(productId).map(mapper::toResult)
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PRODUCT_NOT_FOUND));
    }
}
