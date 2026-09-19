package com.coldchain.modules.identity.internal.application.usecase.query;

import com.coldchain.modules.identity.api.dto.UserResult;
import com.coldchain.modules.identity.internal.application.mapper.IdentityApiMapper;
import com.coldchain.modules.identity.internal.domain.repository.AppUserRepository;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.pagination.PageCriteria;
import com.coldchain.shared.pagination.PagedResult;
import java.util.UUID;

@UseCase
public class ListUsersUseCase {

    private final AppUserRepository users;

    private final IdentityApiMapper mapper;

    public ListUsersUseCase(AppUserRepository users, IdentityApiMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    public PagedResult<UserResult> execute(UUID organizationId, PageCriteria criteria) {
        return users.findByOrganization(organizationId, criteria).map(mapper::toResult);
    }
}
