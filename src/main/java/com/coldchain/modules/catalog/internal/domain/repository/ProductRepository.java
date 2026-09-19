package com.coldchain.modules.catalog.internal.domain.repository;

import com.coldchain.modules.catalog.internal.domain.model.Product;
import com.coldchain.shared.pagination.PageCriteria;
import com.coldchain.shared.pagination.PagedResult;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    PagedResult<Product> findByOrganization(UUID organizationId, PageCriteria criteria);

    long countUsing(UUID storageProfileId);
}
