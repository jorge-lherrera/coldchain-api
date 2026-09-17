package com.coldchain.modules.identity.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.identity.api.ApiClientStatus;
import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.internal.domain.model.ApiClient;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.ApiClientJpaEntity;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ApiClientPersistenceMapper {

    public ApiClientJpaEntity toEntity(ApiClient client) {
        return new ApiClientJpaEntity(client.id(), client.organizationId(), client.clientId(),
                client.secretHash(), client.label(), client.status().name(), client.lastUsedAt());
    }

    public ApiClient toDomain(ApiClientJpaEntity entity, Set<Scope> scopes) {
        return ApiClient.restore(entity.getId(), entity.getOrganizationId(), entity.getClientId(),
                entity.getSecretHash(), entity.getLabel(), ApiClientStatus.valueOf(entity.getStatus()),
                entity.getLastUsedAt(), scopes);
    }
}
