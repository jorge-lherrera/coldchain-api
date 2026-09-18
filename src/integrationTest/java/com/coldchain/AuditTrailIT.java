package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.dto.AuthenticateCommand;
import com.coldchain.modules.identity.api.dto.InviteUserCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.identifier.RawUuid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class AuditTrailIT {

    private static final String PASSWORD = "Audited!Secret42";

    @Autowired
    private IdentityApi identity;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyWriteLeavesItsEntry() {
        RegisterOrganizationResult organization = register();
        identity.authenticate(new AuthenticateCommand(organization.email(), PASSWORD));
        ActingAs.user(organization.administratorId(), organization.organizationId(),
                () -> identity.inviteUser(new InviteUserCommand(organization.organizationId(),
                        "driver-" + UUID.randomUUID().toString().substring(0, 8) + "@audit.test",
                        "Audited Driver", RoleCode.DRIVER)));

        List<String> actions = jdbc.queryForList("""
                SELECT action FROM audit_entry WHERE organization_id = ? ORDER BY occurred_at
                """, String.class, RawUuid.toBytes(organization.organizationId()));

        assertThat(actions)
                .describedAs("three writes leave three entries, in the order they happened")
                .containsExactly("ORGANIZATION_REGISTERED", "USER_AUTHENTICATED", "USER_INVITED");
    }

    @Test
    void theEntryNamesTheActorTheResourceAndWhatChanged() {
        RegisterOrganizationResult organization = register();

        Map<String, Object> entry = jdbc.queryForMap("""
                SELECT actor_type, resource_type, JSON_SERIALIZE(payload) AS payload
                FROM audit_entry WHERE organization_id = ?
                """, RawUuid.toBytes(organization.organizationId()));

        assertThat(entry.get("ACTOR_TYPE")).isEqualTo("USER");
        assertThat(entry.get("RESOURCE_TYPE")).isEqualTo("ORGANIZATION");
        assertThat((String) entry.get("PAYLOAD"))
                .describedAs("an entry that says what happened but not to what is not evidence")
                .contains("taxId");
    }

    @Test
    void aFailedWriteLeavesNoEntryBehind() {
        RegisterOrganizationResult organization = register();
        int before = countEntries(organization.organizationId());

        assertThatThrownBy(() -> ActingAs.user(organization.administratorId(),
                organization.organizationId(),
                () -> identity.inviteUser(new InviteUserCommand(organization.organizationId(),
                        organization.email(), "Duplicate", RoleCode.DRIVER))))
                .isInstanceOf(DomainException.class);

        assertThat(countEntries(organization.organizationId()))
                .describedAs("the entry and the write are one transaction, so neither survives alone")
                .isEqualTo(before);
    }

    private int countEntries(UUID organizationId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_entry WHERE organization_id = ?",
                Integer.class, RawUuid.toBytes(organizationId));
    }

    private RegisterOrganizationResult register() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return identity.registerOrganization(new RegisterOrganizationCommand(
                "TAX-" + suffix, "Audited Labs S.A.", "Audited", OrganizationKind.LAB, "UY",
                "admin-" + suffix + "@audit.test", "Audited Admin", PASSWORD));
    }
}
