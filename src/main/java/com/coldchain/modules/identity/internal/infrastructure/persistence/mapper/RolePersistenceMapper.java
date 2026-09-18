package com.coldchain.modules.identity.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.internal.domain.model.Role;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.RoleJpaEntity;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RolePersistenceMapper {

    public Role toDomain(RoleJpaEntity entity, Set<Scope> scopes) {
        return Role.restore(entity.getId(), RoleCode.valueOf(entity.getCode()), entity.getName(),
                entity.builtIn(), scopes);
    }
}
