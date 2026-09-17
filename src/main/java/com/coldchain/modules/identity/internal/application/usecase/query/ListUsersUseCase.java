package com.coldchain.modules.identity.internal.application.usecase.query;

import com.coldchain.modules.identity.api.dto.UserResult;
import com.coldchain.modules.identity.internal.application.mapper.IdentityApiMapper;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.shared.application.UseCase;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@UseCase
public class ListUsersUseCase {

    private final AppUserRepository users;

    private final IdentityApiMapper mapper;

    public ListUsersUseCase(AppUserRepository users, IdentityApiMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    public Page<UserResult> execute(UUID organizationId, Pageable pageable) {
        return users.findByOrganization(organizationId, pageable).map(mapper::toResult);
    }
}
