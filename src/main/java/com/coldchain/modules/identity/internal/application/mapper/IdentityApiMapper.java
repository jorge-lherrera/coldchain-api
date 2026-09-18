package com.coldchain.modules.identity.internal.application.mapper;

import com.coldchain.modules.identity.api.dto.OrganizationResult;
import com.coldchain.modules.identity.api.dto.UserResult;
import com.coldchain.modules.identity.internal.domain.model.AppUser;
import com.coldchain.modules.identity.internal.domain.model.Organization;
import org.springframework.stereotype.Component;

@Component
public class IdentityApiMapper {

    public UserResult toResult(AppUser user) {
        return new UserResult(user.id(), user.organizationId(), user.email(), user.fullName(), user.status(),
                user.lastLoginAt());
    }

    public OrganizationResult toResult(Organization organization) {
        return new OrganizationResult(organization.id(), organization.taxId(), organization.legalName(),
                organization.tradeName(), organization.kind(), organization.country(), organization.status());
    }
}
