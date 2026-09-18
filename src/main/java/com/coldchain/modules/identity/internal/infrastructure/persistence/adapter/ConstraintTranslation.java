package com.coldchain.modules.identity.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.error.DomainException;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;

final class ConstraintTranslation {

    private static final Map<String, IdentityErrorCode> BY_CONSTRAINT = Map.of(
            "UQ_ORGANIZATION_TAX_ID", IdentityErrorCode.TAX_ID_ALREADY_REGISTERED,
            "UX_APP_USER_EMAIL_LOWER", IdentityErrorCode.EMAIL_ALREADY_REGISTERED,
            "UQ_API_CLIENT_CLIENT_ID", IdentityErrorCode.CLIENT_ID_ALREADY_REGISTERED);

    private ConstraintTranslation() {
    }

    static RuntimeException translate(DataIntegrityViolationException cause) {
        String message = String.valueOf(cause.getMostSpecificCause().getMessage()).toUpperCase(Locale.ROOT);
        return BY_CONSTRAINT.entrySet().stream()
                .filter(entry -> message.contains(entry.getKey()))
                .<RuntimeException>map(entry -> DomainException.of(entry.getValue()))
                .findFirst()
                .orElse(cause);
    }
}
