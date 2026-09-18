package com.coldchain.modules.catalog.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.catalog.internal.domain.model.Product;
import com.coldchain.modules.catalog.internal.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {

    public ProductJpaEntity toEntity(Product product) {
        return new ProductJpaEntity(product.id(), product.organizationId(), product.storageProfileId(),
                product.sku(), product.name(), product.deletedAt(), product.lockVersion());
    }

    public Product toDomain(ProductJpaEntity entity) {
        return Product.restore(entity.getId(), entity.getOrganizationId(), entity.getStorageProfileId(),
                entity.getSku(), entity.getName(), entity.getDeletedAt(), entity.getLockVersion());
    }
}
