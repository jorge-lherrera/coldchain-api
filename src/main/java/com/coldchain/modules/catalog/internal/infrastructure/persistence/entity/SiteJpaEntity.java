package com.coldchain.modules.catalog.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "SITE", indexes = {
        @Index(name = "IX_SITE_ORGANIZATION_ID", columnList = "ORGANIZATION_ID")})
public class SiteJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "KIND", nullable = false)
    private String kind;

    @Column(name = "LATITUDE", nullable = false)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private BigDecimal longitude;

    @Column(name = "TIME_ZONE", nullable = false)
    private String timeZone;

    @Column(name = "DELETED_AT")
    private Instant deletedAt;

    protected SiteJpaEntity() {
    }

    public SiteJpaEntity(UUID id, UUID organizationId, String code, String name, String kind,
            BigDecimal latitude, BigDecimal longitude, String timeZone, Instant deletedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.code = code;
        this.name = name;
        this.kind = kind;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZone = timeZone;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
