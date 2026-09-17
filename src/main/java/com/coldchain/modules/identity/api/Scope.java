package com.coldchain.modules.identity.api;

import java.util.Optional;
import java.util.stream.Stream;

public enum Scope {

    ORGANIZATION_READ,
    ORGANIZATION_WRITE,
    USER_READ,
    USER_WRITE,
    ROLE_WRITE,
    CATALOG_READ,
    CATALOG_WRITE,
    SHIPMENT_READ,
    SHIPMENT_WRITE,
    SHIPMENT_DISPATCH,
    SHIPMENT_HANDOFF,
    DEVICE_READ,
    DEVICE_WRITE,
    TELEMETRY_INGEST,
    TELEMETRY_READ,
    COMPLIANCE_READ,
    COMPLIANCE_ISSUE;

    public static Optional<Scope> ofCode(String code) {
        return Stream.of(values()).filter(scope -> scope.name().equals(code)).findFirst();
    }
}
