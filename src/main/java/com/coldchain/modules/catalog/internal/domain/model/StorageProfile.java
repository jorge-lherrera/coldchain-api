package com.coldchain.modules.catalog.internal.domain.model;

import com.coldchain.modules.catalog.api.ProfileStatus;
import com.coldchain.shared.identifier.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class StorageProfile {

    private static final int FIRST_VERSION = 1;

    private final UUID id;

    private final UUID organizationId;

    private final String code;

    private final String name;

    private final int version;

    private final ProfileStatus status;

    private final Thresholds thresholds;

    private final Instant deletedAt;

    private StorageProfile(UUID id, UUID organizationId, String code, String name, int version,
            ProfileStatus status, Thresholds thresholds, Instant deletedAt) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
        this.version = version;
        this.status = Objects.requireNonNull(status);
        this.thresholds = Objects.requireNonNull(thresholds);
        this.deletedAt = deletedAt;
    }

    public static StorageProfile createDraft(UUID organizationId, String code, String name,
            Thresholds thresholds) {
        return new StorageProfile(UuidV7.generate(), organizationId, code, name, FIRST_VERSION,
                ProfileStatus.DRAFT, thresholds, null);
    }

    public static StorageProfile restore(UUID id, UUID organizationId, String code, String name,
            int version, ProfileStatus status, Thresholds thresholds, Instant deletedAt) {
        return new StorageProfile(id, organizationId, code, name, version, status, thresholds, deletedAt);
    }

    public StorageProfile activate() {
        return new StorageProfile(id, organizationId, code, name, version, ProfileStatus.ACTIVE,
                thresholds, deletedAt);
    }

    public StorageProfile retire() {
        return new StorageProfile(id, organizationId, code, name, version, ProfileStatus.RETIRED,
                thresholds, deletedAt);
    }

    public StorageProfile redraft(String newName, Thresholds newThresholds) {
        return new StorageProfile(id, organizationId, code, newName, version, status, newThresholds,
                deletedAt);
    }

    public StorageProfile nextVersion() {
        return new StorageProfile(UuidV7.generate(), organizationId, code, name, version + 1,
                ProfileStatus.DRAFT, thresholds, null);
    }

    public boolean editable() {
        return status == ProfileStatus.DRAFT;
    }

    public boolean usable() {
        return status == ProfileStatus.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public int version() {
        return version;
    }

    public ProfileStatus status() {
        return status;
    }

    public Thresholds thresholds() {
        return thresholds;
    }

    public Instant deletedAt() {
        return deletedAt;
    }
}
