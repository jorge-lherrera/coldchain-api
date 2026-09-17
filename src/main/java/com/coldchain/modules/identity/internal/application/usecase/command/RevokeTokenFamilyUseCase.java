package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.internal.domain.repository.RefreshTokenRepository;
import com.coldchain.shared.application.UseCase;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RevokeTokenFamilyUseCase {

    private final RefreshTokenRepository refreshTokens;

    private final Clock clock;

    public RevokeTokenFamilyUseCase(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int execute(UUID familyId) {
        return refreshTokens.revokeFamily(familyId, clock.instant());
    }
}
