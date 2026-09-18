package com.coldchain.modules.catalog.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.error.DomainException;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;

final class CatalogConstraintTranslation {

    private static final Map<String, CatalogErrorCode> BY_CONSTRAINT = Map.of(
            "UX_STORAGE_PROFILE_CODE_LIVE", CatalogErrorCode.PROFILE_CODE_ALREADY_IN_USE,
            "UX_PRODUCT_SKU_LIVE", CatalogErrorCode.SKU_ALREADY_IN_USE,
            "UX_SITE_CODE_LIVE", CatalogErrorCode.SITE_CODE_ALREADY_IN_USE);

    private CatalogConstraintTranslation() {
    }

    static RuntimeException translate(DataIntegrityViolationException cause) {
        String message = cause.getMostSpecificCause().getMessage().toUpperCase(Locale.ROOT);
        return BY_CONSTRAINT.entrySet().stream()
                .filter(entry -> message.contains(entry.getKey()))
                .findFirst()
                .<RuntimeException>map(entry -> DomainException.of(entry.getValue()))
                .orElse(cause);
    }
}
