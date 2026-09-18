package com.coldchain.modules.catalog.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.catalog.internal.domain.model.Product;
import com.coldchain.modules.catalog.internal.domain.repository.ProductRepository;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.jpa.ProductJpaRepository;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.mapper.ProductPersistenceMapper;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import com.coldchain.shared.paging.SpringDataPaging;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository products;

    private final ProductPersistenceMapper mapper;

    public ProductRepositoryAdapter(ProductJpaRepository products, ProductPersistenceMapper mapper) {
        this.products = products;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {
        try {
            products.saveAndFlush(mapper.toEntity(product));
        } catch (DataIntegrityViolationException cause) {
            throw CatalogConstraintTranslation.translate(cause);
        }
        return product;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return products.findById(id).map(mapper::toDomain);
    }

    @Override
    public PagedResult<Product> findByOrganization(UUID organizationId, PageCriteria criteria) {
        return SpringDataPaging.toPagedResult(
                products.findByOrganizationIdAndDeletedAtIsNull(organizationId,
                        SpringDataPaging.toPageable(criteria)).map(mapper::toDomain),
                criteria);
    }

    @Override
    public long countUsing(UUID storageProfileId) {
        return products.countByStorageProfileIdAndDeletedAtIsNull(storageProfileId);
    }
}
