package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.Scope;
import com.coldchain.modules.identity.api.dto.ApiClientResult;
import com.coldchain.modules.identity.api.dto.CreateApiClientCommand;
import com.coldchain.modules.identity.api.event.ApiClientCreated;
import com.coldchain.modules.identity.internal.application.CurrentIdentityActor;
import com.coldchain.modules.identity.internal.domain.model.ApiClient;
import com.coldchain.modules.identity.internal.domain.repository.ApiClientRepository;
import com.coldchain.modules.identity.internal.domain.repository.OrganizationRepository;
import com.coldchain.modules.identity.internal.domain.service.IssuedSecret;
import com.coldchain.modules.identity.internal.domain.service.OpaqueTokenFactory;
import com.coldchain.modules.identity.internal.domain.service.SecretHasher;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.time.Clock;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class CreateApiClientUseCase {

    private static final Set<Scope> ALLOWED_FOR_MACHINES = Set.of(Scope.TELEMETRY_INGEST);

    private final ApiClientRepository clients;

    private final OrganizationRepository organizations;

    private final OpaqueTokenFactory tokens;

    private final SecretHasher hasher;

    private final CurrentIdentityActor identityActor;

    private final ApplicationEventPublisher events;

    private final Clock clock;

    public CreateApiClientUseCase(ApiClientRepository clients, OrganizationRepository organizations,
            OpaqueTokenFactory tokens, SecretHasher hasher, CurrentIdentityActor identityActor,
            ApplicationEventPublisher events, Clock clock) {
        this.clients = clients;
        this.organizations = organizations;
        this.tokens = tokens;
        this.hasher = hasher;
        this.identityActor = identityActor;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ApiClientResult execute(CreateApiClientCommand command) {
        organizations.findById(command.organizationId())
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.ORGANIZATION_NOT_FOUND));
        if (!ALLOWED_FOR_MACHINES.containsAll(command.scopes())) {
            throw DomainException.of(IdentityErrorCode.SCOPE_NOT_ALLOWED_FOR_CLIENT);
        }
        IssuedSecret credentials = tokens.issue();
        IssuedSecret identifier = tokens.issue();
        ApiClient client = clients.save(ApiClient.createNew(command.organizationId(),
                identifier.plainToken(), hasher.hash(credentials.plainToken()), command.label(),
                command.scopes()));
        events.publishEvent(new ApiClientCreated(command.organizationId(), identityActor.type(),
                identityActor.id(), client.id(), client.label(), client.scopes(), clock.instant()));
        return new ApiClientResult(client.id(), client.clientId(), credentials.plainToken(), client.label(),
                client.status(), client.scopes());
    }
}
