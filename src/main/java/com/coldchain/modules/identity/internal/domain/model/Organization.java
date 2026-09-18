package com.coldchain.modules.identity.internal.domain.model;

import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.OrganizationStatus;
import com.coldchain.shared.identifier.UuidV7;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Organization {

    private final UUID id;

    private final String taxId;

    private final String legalName;

    private final String tradeName;

    private final OrganizationKind kind;

    private final String country;

    private final OrganizationStatus status;

    private Organization(UUID id, String taxId, String legalName, String tradeName, OrganizationKind kind,
            String country, OrganizationStatus status) {
        this.id = Objects.requireNonNull(id);
        this.taxId = requireText(taxId, "taxId");
        this.legalName = requireText(legalName, "legalName");
        this.tradeName = tradeName;
        this.kind = Objects.requireNonNull(kind);
        this.country = requireCountry(country);
        this.status = Objects.requireNonNull(status);
    }

    public static Organization createNew(String taxId, String legalName, String tradeName,
            OrganizationKind kind, String country) {
        return new Organization(UuidV7.generate(), taxId, legalName, tradeName, kind, country,
                OrganizationStatus.ACTIVE);
    }

    public static Organization restore(UUID id, String taxId, String legalName, String tradeName,
            OrganizationKind kind, String country, OrganizationStatus status) {
        return new Organization(id, taxId, legalName, tradeName, kind, country, status);
    }

    public Organization suspend() {
        return new Organization(id, taxId, legalName, tradeName, kind, country, OrganizationStatus.SUSPENDED);
    }

    public Organization reinstate() {
        return new Organization(id, taxId, legalName, tradeName, kind, country, OrganizationStatus.ACTIVE);
    }

    public boolean active() {
        return status == OrganizationStatus.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public String taxId() {
        return taxId;
    }

    public String legalName() {
        return legalName;
    }

    public String tradeName() {
        return tradeName;
    }

    public OrganizationKind kind() {
        return kind;
    }

    public String country() {
        return country;
    }

    public OrganizationStatus status() {
        return status;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("An organization needs a " + field);
        }
        return value.trim();
    }

    private static String requireCountry(String value) {
        String country = requireText(value, "country");
        if (country.length() != 2) {
            throw new IllegalArgumentException("A country is an ISO 3166-1 alpha-2 code, received " + country);
        }
        return country.toUpperCase(Locale.ROOT);
    }
}
