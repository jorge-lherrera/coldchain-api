package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.IdentityApi;
import com.coldchain.modules.identity.api.OrganizationKind;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.shared.util.RawUuid;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UuidRawBindingIT {

    @Autowired
    private IdentityApi identity;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theSixteenBytesAreTheSameOnesHibernateWrites() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterOrganizationResult written = identity.registerOrganization(
                new RegisterOrganizationCommand("TAX-" + suffix, "Binding Labs S.A.", "Binding",
                        OrganizationKind.LAB, "UY", "admin-" + suffix + "@binding.test",
                        "Binding Admin", "Binding!Secret42"));

        String stored = jdbc.queryForObject(
                "SELECT RAWTOHEX(id) FROM organization WHERE id = ?", String.class,
                RawUuid.toBytes(written.organizationId()));
        String expected = HexFormat.of().withUpperCase()
                .formatHex(RawUuid.toBytes(written.organizationId()));

        assertThat(stored)
                .describedAs("a reordered byte is an identifier that no native query can find again")
                .isEqualTo(expected);
        assertThat(RawUuid.fromBytes(HexFormat.of().parseHex(stored)))
                .isEqualTo(written.organizationId());
    }
}
