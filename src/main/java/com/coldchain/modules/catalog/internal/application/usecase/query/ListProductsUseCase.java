package com.coldchain.modules.catalog.internal.application.usecase.query;

import com.coldchain.modules.catalog.api.dto.ProductResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.repository.ProductRepository;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;

@UseCase
public class ListProductsUseCase {

    private final ProductRepository products;

    private final CatalogApiMapper mapper;

    public ListProductsUseCase(ProductRepository products, CatalogApiMapper mapper) {
        this.products = products;
        this.mapper = mapper;
    }

    public PagedResult<ProductResult> execute(UUID organizationId, PageCriteria criteria) {
        return products.findByOrganization(organizationId, criteria).map(mapper::toResult);
    }
}
