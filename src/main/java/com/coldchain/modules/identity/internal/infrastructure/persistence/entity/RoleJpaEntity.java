package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import com.coldchain.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ROLE")
public class RoleJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "BUILT_IN", nullable = false)
    private boolean builtIn;

    protected RoleJpaEntity() {
    }

    public RoleJpaEntity(UUID id, String code, String name, boolean builtIn) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.builtIn = builtIn;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean builtIn() {
        return builtIn;
    }
}
