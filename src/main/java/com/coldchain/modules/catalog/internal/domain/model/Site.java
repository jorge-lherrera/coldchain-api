package com.coldchain.modules.catalog.internal.domain.model;

import com.coldchain.modules.catalog.api.SiteKind;
import com.coldchain.shared.identifier.UuidV7;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public final class Site {

    private final UUID id;

    private final UUID organizationId;

    private final String code;

    private final String name;

    private final SiteKind kind;

    private final BigDecimal latitude;

    private final BigDecimal longitude;

    private final String timeZone;

    private final Instant deletedAt;

    private Site(UUID id, UUID organizationId, String code, String name, SiteKind kind,
            BigDecimal latitude, BigDecimal longitude, String timeZone, Instant deletedAt) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
        this.kind = Objects.requireNonNull(kind);
        this.latitude = Objects.requireNonNull(latitude);
        this.longitude = Objects.requireNonNull(longitude);
        this.timeZone = Objects.requireNonNull(timeZone);
        this.deletedAt = deletedAt;
    }

    public static Site createNew(UUID organizationId, String code, String name, SiteKind kind,
            BigDecimal latitude, BigDecimal longitude, String timeZone) {
        return new Site(UuidV7.generate(), organizationId, code, name, kind, latitude, longitude,
                requireKnownZone(timeZone), null);
    }

    public static Site restore(UUID id, UUID organizationId, String code, String name, SiteKind kind,
            BigDecimal latitude, BigDecimal longitude, String timeZone, Instant deletedAt) {
        return new Site(id, organizationId, code, name, kind, latitude, longitude, timeZone, deletedAt);
    }

    public Site relocatedTo(String newName, BigDecimal newLatitude, BigDecimal newLongitude,
            String newTimeZone) {
        return new Site(id, organizationId, code, newName, kind, newLatitude, newLongitude,
                requireKnownZone(newTimeZone), deletedAt);
    }

    private static String requireKnownZone(String candidate) {
        if (!ZoneId.getAvailableZoneIds().contains(candidate)) {
            throw new IllegalArgumentException(
                    "A site keeps an IANA zone identifier, not an offset: " + candidate);
        }
        return candidate;
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

    public SiteKind kind() {
        return kind;
    }

    public BigDecimal latitude() {
        return latitude;
    }

    public BigDecimal longitude() {
        return longitude;
    }

    public String timeZone() {
        return timeZone;
    }

    public Instant deletedAt() {
        return deletedAt;
    }
}
