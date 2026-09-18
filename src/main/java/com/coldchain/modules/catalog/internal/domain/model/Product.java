package com.coldchain.modules.catalog.internal.domain.model;

import com.coldchain.shared.identifier.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Product {

    private final UUID id;

    private final UUID organizationId;

    private final UUID storageProfileId;

    private final String sku;

    private final String name;

    private final Instant deletedAt;

    private final long lockVersion;

    private Product(UUID id, UUID organizationId, UUID storageProfileId, String sku, String name,
            Instant deletedAt, long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.storageProfileId = Objects.requireNonNull(storageProfileId);
        this.sku = Objects.requireNonNull(sku);
        this.name = Objects.requireNonNull(name);
        this.deletedAt = deletedAt;
        this.lockVersion = lockVersion;
    }

    public static Product createNew(UUID organizationId, UUID storageProfileId, String sku, String name) {
        return new Product(UuidV7.generate(), organizationId, storageProfileId, sku, name, null, 0);
    }

    public static Product restore(UUID id, UUID organizationId, UUID storageProfileId, String sku,
            String name, Instant deletedAt, long lockVersion) {
        return new Product(id, organizationId, storageProfileId, sku, name, deletedAt, lockVersion);
    }

    public Product describedAs(String newName, UUID newStorageProfileId) {
        return new Product(id, organizationId, newStorageProfileId, sku, newName, deletedAt, lockVersion);
    }

    public Product retire(Instant when) {
        return new Product(id, organizationId, storageProfileId, sku, name, when, lockVersion);
    }

    public boolean retired() {
        return deletedAt != null;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID storageProfileId() {
        return storageProfileId;
    }

    public String sku() {
        return sku;
    }

    public String name() {
        return name;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public long lockVersion() {
        return lockVersion;
    }
}
