package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import com.coldchain.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "ORGANIZATION")
public class OrganizationJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "TAX_ID", nullable = false)
    private String taxId;

    @Column(name = "LEGAL_NAME", nullable = false)
    private String legalName;

    @Column(name = "TRADE_NAME")
    private String tradeName;

    @Column(name = "KIND", nullable = false)
    private String kind;

    @Column(name = "COUNTRY", nullable = false)
    private String country;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected OrganizationJpaEntity() {
    }

    public OrganizationJpaEntity(UUID id, String taxId, String legalName, String tradeName, String kind, String country, String status, long lockVersion) {
        this.id = id;
        this.taxId = taxId;
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.kind = kind;
        this.country = country;
        this.status = status;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getTradeName() {
        return tradeName;
    }

    public String getKind() {
        return kind;
    }

    public String getCountry() {
        return country;
    }

    public String getStatus() {
        return status;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
