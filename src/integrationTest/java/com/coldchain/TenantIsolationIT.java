package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.dto.InviteUserCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.identity.api.dto.UserResult;
import com.coldchain.shared.pagination.PageCriteria;
import com.coldchain.shared.pagination.PagedResult;
import com.coldchain.shared.pagination.SortDirection;
import com.coldchain.shared.pagination.SortOrder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class TenantIsolationIT {

    private static final PageCriteria FIRST_PAGE =
            new PageCriteria(0, 50, List.of(new SortOrder("email", SortDirection.ASC)));

    @Autowired
    private IdentityApi identity;

    @Test
    void anOrganizationOnlySeesItsOwnPeople() {
        RegisterOrganizationResult mine = register("mine");
        RegisterOrganizationResult theirs = register("theirs");
        String myDriver = invite(mine);
        String theirDriver = invite(theirs);

        PagedResult<UserResult> seen = identity.listUsers(mine.organizationId(), FIRST_PAGE);

        assertThat(seen.content()).extracting(UserResult::email)
                .describedAs("a listing that leaks one row of another tenant has leaked the tenant")
                .contains(mine.email(), myDriver)
                .doesNotContain(theirs.email(), theirDriver);
        assertThat(seen.content()).extracting(UserResult::organizationId)
                .containsOnly(mine.organizationId());
    }

    @Test
    void theOtherOrganizationSeesTheMirrorImage() {
        RegisterOrganizationResult mine = register("mine");
        RegisterOrganizationResult theirs = register("theirs");
        invite(mine);
        String theirDriver = invite(theirs);

        PagedResult<UserResult> seen = identity.listUsers(theirs.organizationId(), FIRST_PAGE);

        assertThat(seen.content()).extracting(UserResult::email)
                .contains(theirs.email(), theirDriver)
                .doesNotContain(mine.email());
        assertThat(seen.totalElements())
                .describedAs("the count belongs to the tenant, not to the table")
                .isEqualTo(2);
    }

    private RegisterOrganizationResult register(String who) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, who + " Logistics S.A.", who, OrganizationKind.CARRIER, "UY",
                who + "-admin-" + suffix + "@tenant.test", who + " Admin", "Tenant!Secret42"));
    }

    private String invite(RegisterOrganizationResult organization) {
        String email = "driver-" + UUID.randomUUID().toString().substring(0, 8) + "@tenant.test";
        return ActingAs.user(organization.administratorId(), organization.organizationId(),
                () -> identity.inviteUser(new InviteUserCommand(organization.organizationId(), email,
                        "Invited Driver", RoleCode.DRIVER))).email();
    }
}
