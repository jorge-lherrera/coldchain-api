package com.coldchain.modules.identity.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.internal.domain.model.ApiClient;
import com.coldchain.modules.identity.internal.domain.repository.ApiClientRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.ApiClientJpaEntity;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.ApiClientScopeJpaEntity;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.ApiClientJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.jpa.ApiClientScopeJpaRepository;
import com.coldchain.modules.identity.internal.infrastructure.persistence.mapper.ApiClientPersistenceMapper;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class ApiClientRepositoryAdapter implements ApiClientRepository {

    private final ApiClientJpaRepository clients;

    private final ApiClientScopeJpaRepository clientScopes;

    private final ApiClientPersistenceMapper mapper;

    public ApiClientRepositoryAdapter(ApiClientJpaRepository clients, ApiClientScopeJpaRepository clientScopes,
            ApiClientPersistenceMapper mapper) {
        this.clients = clients;
        this.clientScopes = clientScopes;
        this.mapper = mapper;
    }

    @Override
    public ApiClient save(ApiClient client) {
        try {
            ApiClientJpaEntity saved = clients.saveAndFlush(mapper.toEntity(client));
            clientScopes.deleteByApiClientId(saved.getId());
            clientScopes.saveAllAndFlush(client.scopes().stream()
                    .map(scope -> new ApiClientScopeJpaEntity(saved.getId(), scope.name()))
                    .toList());
            return mapper.toDomain(saved, client.scopes());
        } catch (DataIntegrityViolationException cause) {
            throw ConstraintTranslation.translate(cause);
        }
    }

    @Override
    public Optional<ApiClient> findById(UUID id) {
        return clients.findById(id).map(entity -> mapper.toDomain(entity, scopesOf(entity.getId())));
    }

    @Override
    public Optional<ApiClient> findByClientId(String clientId) {
        return clients.findByClientId(clientId)
                .map(entity -> mapper.toDomain(entity, scopesOf(entity.getId())));
    }

    @Override
    public Page<ApiClient> findByOrganization(UUID organizationId, Pageable pageable) {
        return clients.findByOrganizationId(organizationId, pageable)
                .map(entity -> mapper.toDomain(entity, scopesOf(entity.getId())));
    }

    private Set<Scope> scopesOf(UUID apiClientId) {
        List<ApiClientScopeJpaEntity> rows = clientScopes.findByApiClientId(apiClientId);
        Set<Scope> scopes = EnumSet.noneOf(Scope.class);
        rows.stream().map(ApiClientScopeJpaEntity::getScopeCode).map(Scope::ofCode)
                .flatMap(Optional::stream).forEach(scopes::add);
        return Set.copyOf(scopes);
    }
}
