package com.coldchain.modules.catalog.internal.application.usecase.command;

import com.coldchain.modules.catalog.internal.domain.model.Product;
import com.coldchain.modules.catalog.internal.domain.repository.ProductRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RetireProductUseCase {

    private final ProductRepository products;

    private final Clock clock;

    public RetireProductUseCase(ProductRepository products, Clock clock) {
        this.products = products;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PRODUCT_NOT_FOUND));
        products.save(product.retire(clock.instant()));
    }
}
