package com.coldchain.modules.identity.internal.domain.repository;

import com.coldchain.modules.identity.internal.domain.model.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppUserRepository {

    AppUser save(AppUser user);

    Optional<AppUser> findById(UUID id);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByActivationTokenHash(String activationTokenHash);

    Page<AppUser> findByOrganization(UUID organizationId, Pageable pageable);

    long countAdministrators(UUID organizationId);
}
