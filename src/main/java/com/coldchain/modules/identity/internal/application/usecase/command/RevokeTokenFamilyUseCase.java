package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.api.event.RefreshTokenReuseDetected;
import com.coldchain.modules.identity.internal.domain.model.RefreshToken;
import com.coldchain.modules.identity.internal.domain.repository.RefreshTokenRepository;
import com.coldchain.shared.application.UseCase;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RevokeTokenFamilyUseCase {

    private final RefreshTokenRepository refreshTokens;

    private final ApplicationEventPublisher events;

    private final Clock clock;

    public RevokeTokenFamilyUseCase(RefreshTokenRepository refreshTokens,
            ApplicationEventPublisher events, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int execute(RefreshToken reused) {
        Instant now = clock.instant();
        int revoked = refreshTokens.revokeFamily(reused.familyId(), now);
        events.publishEvent(new RefreshTokenReuseDetected(reused.organizationId(), ActorType.USER,
                reused.appUserId(), reused.familyId(), revoked, now));
        return revoked;
    }
}
