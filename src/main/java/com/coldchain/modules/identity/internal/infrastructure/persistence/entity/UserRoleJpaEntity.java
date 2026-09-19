package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import com.coldchain.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(UserRoleId.class)
@Table(name = "USER_ROLE",
        indexes = {@Index(name = "IX_USER_ROLE_ROLE_ID", columnList = "ROLE_ID"), @Index(name = "IX_USER_ROLE_GRANTED_BY", columnList = "GRANTED_BY")})
public class UserRoleJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "APP_USER_ID", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "ROLE_ID", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "GRANTED_BY")
    private UUID grantedBy;

    @Column(name = "GRANTED_AT", nullable = false)
    private Instant grantedAt;

    protected UserRoleJpaEntity() {
    }

    public UserRoleJpaEntity(UUID userId, UUID roleId, UUID grantedBy, Instant grantedAt) {
        this.userId = userId;
        this.roleId = roleId;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }
}
