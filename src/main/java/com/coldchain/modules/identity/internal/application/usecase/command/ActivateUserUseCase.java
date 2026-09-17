package com.coldchain.modules.identity.internal.application.usecase.command;

import com.coldchain.modules.identity.api.dto.ActivateUserCommand;
import com.coldchain.modules.identity.api.dto.ActivateUserResult;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.modules.identity.internal.domain.service.OpaqueTokenFactory;
import com.coldchain.modules.identity.internal.domain.service.SecretHasher;
import com.coldchain.modules.identity.internal.exception.IdentityErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ActivateUserUseCase {

    private final AppUserRepository users;

    private final OpaqueTokenFactory tokens;

    private final SecretHasher hasher;

    private final Clock clock;

    public ActivateUserUseCase(AppUserRepository users, OpaqueTokenFactory tokens, SecretHasher hasher,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Transactional
    public ActivateUserResult execute(ActivateUserCommand command) {
        AppUser user = users.findByActivationTokenHash(tokens.fingerprint(command.activationToken()))
                .orElseThrow(() -> DomainException.of(IdentityErrorCode.ACTIVATION_TOKEN_INVALID));
        if (user.active()) {
            throw DomainException.of(IdentityErrorCode.USER_ALREADY_ACTIVE);
        }
        if (user.activationExpiredAt(clock.instant())) {
            throw DomainException.of(IdentityErrorCode.ACTIVATION_TOKEN_EXPIRED);
        }
        AppUser activated = users.save(user.activate(hasher.hash(command.password())));
        return new ActivateUserResult(activated.id(), activated.email());
    }
}
