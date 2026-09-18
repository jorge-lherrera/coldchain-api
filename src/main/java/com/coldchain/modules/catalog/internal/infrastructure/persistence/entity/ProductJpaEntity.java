package com.coldchain.modules.catalog.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "PRODUCT", indexes = {
        @Index(name = "IX_PRODUCT_ORGANIZATION_ID", columnList = "ORGANIZATION_ID"),
        @Index(name = "IX_PRODUCT_STORAGE_PROFILE_ID", columnList = "STORAGE_PROFILE_ID")})
public class ProductJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "STORAGE_PROFILE_ID", nullable = false)
    private UUID storageProfileId;

    @Column(name = "SKU", nullable = false)
    private String sku;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DELETED_AT")
    private Instant deletedAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(UUID id, UUID organizationId, UUID storageProfileId, String sku, String name,
            Instant deletedAt, long lockVersion) {
        this.id = id;
        this.organizationId = organizationId;
        this.storageProfileId = storageProfileId;
        this.sku = sku;
        this.name = name;
        this.deletedAt = deletedAt;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getStorageProfileId() {
        return storageProfileId;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
