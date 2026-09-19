package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import com.coldchain.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@IdClass(RoleScopeId.class)
@Table(name = "ROLE_SCOPE")
public class RoleScopeJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ROLE_ID", nullable = false, updatable = false)
    private UUID roleId;

    @Id
    @Column(name = "SCOPE_CODE", nullable = false, updatable = false)
    private String scopeCode;

    protected RoleScopeJpaEntity() {
    }

    public RoleScopeJpaEntity(UUID roleId, String scopeCode) {
        this.roleId = roleId;
        this.scopeCode = scopeCode;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getScopeCode() {
        return scopeCode;
    }
}
