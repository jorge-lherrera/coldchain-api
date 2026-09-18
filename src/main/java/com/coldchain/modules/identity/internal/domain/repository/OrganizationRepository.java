package com.coldchain.modules.identity.internal.domain.repository;

import com.coldchain.modules.identity.internal.domain.model.Organization;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {

    Organization save(Organization organization);

    Optional<Organization> findById(UUID id);

    Optional<Organization> findByTaxId(String taxId);
}
