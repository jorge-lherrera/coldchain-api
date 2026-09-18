package com.coldchain.modules.identity.internal.domain.repository;

import com.coldchain.modules.identity.internal.domain.model.AppUser;
import java.util.Optional;
import java.util.UUID;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;

public interface AppUserRepository {

    AppUser save(AppUser user);

    Optional<AppUser> findById(UUID id);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByActivationTokenHash(String activationTokenHash);

    PagedResult<AppUser> findByOrganization(UUID organizationId, PageCriteria criteria);

    long countAdministrators(UUID organizationId);
}
