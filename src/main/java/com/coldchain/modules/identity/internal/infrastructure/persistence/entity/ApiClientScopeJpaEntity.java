package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@IdClass(ApiClientScopeId.class)
@Table(name = "API_CLIENT_SCOPE")
public class ApiClientScopeJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "API_CLIENT_ID", nullable = false, updatable = false)
    private UUID apiClientId;

    @Id
    @Column(name = "SCOPE_CODE", nullable = false, updatable = false)
    private String scopeCode;

    protected ApiClientScopeJpaEntity() {
    }

    public ApiClientScopeJpaEntity(UUID apiClientId, String scopeCode) {
        this.apiClientId = apiClientId;
        this.scopeCode = scopeCode;
    }

    public UUID getApiClientId() {
        return apiClientId;
    }

    public String getScopeCode() {
        return scopeCode;
    }
}
