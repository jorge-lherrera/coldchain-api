package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.dto.IssueClientTokenCommand;
import com.coldchain.modules.identity.api.dto.TokenResult;
import com.coldchain.modules.identity.api.event.ClientTokenIssued;
import com.coldchain.modules.identity.internal.domain.model.ApiClient;
import com.coldchain.modules.identity.internal.domain.repository.ApiClientRepository;
import com.coldchain.modules.identity.internal.domain.service.AccessTokenIssuer;
import com.coldchain.modules.identity.internal.domain.service.SecretHasher;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class IssueClientTokenUseCase {

    private final ApiClientRepository clients;

    private final SecretHasher hasher;

    private final AccessTokenIssuer accessTokens;

    private final ApplicationEventPublisher events;

    private final Clock clock;

    public IssueClientTokenUseCase(ApiClientRepository clients, SecretHasher hasher,
            AccessTokenIssuer accessTokens, ApplicationEventPublisher events, Clock clock) {
        this.clients = clients;
        this.hasher = hasher;
        this.accessTokens = accessTokens;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public TokenResult execute(IssueClientTokenCommand command) {
        ApiClient client = clients.findByClientId(command.clientId())
                .filter(found -> hasher.matches(command.clientSecret(), found.secretHash()))
                .filter(ApiClient::active)
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.INVALID_CLIENT_CREDENTIALS));
        clients.save(client.recordUse(clock.instant()));
        String accessToken = accessTokens.issue(ActorType.CLIENT, client.id(), client.organizationId(),
                client.scopes());
        events.publishEvent(new ClientTokenIssued(client.organizationId(), ActorType.CLIENT, client.id(),
                clock.instant()));
        return new TokenResult(accessToken, null, "Bearer", accessTokens.accessTokenLifetime().toSeconds());
    }
}
