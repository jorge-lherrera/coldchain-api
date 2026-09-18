package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.RoleCode;
import com.coldchain.modules.identity.api.Scope;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class ScopeCatalogueIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theSeededScopesMatchTheEnum() {
        List<String> catalogue = Arrays.stream(Scope.values()).map(Enum::name).toList();
        List<String> seeded = jdbc.queryForList(
                "SELECT DISTINCT scope_code FROM role_scope", String.class);

        assertThat(seeded).describedAs("the migration seeds the permissions of the built-in roles")
                .isNotEmpty();
        assertThat(catalogue)
                .describedAs("a scope seeded with a name the enum does not know grants nothing and "
                        + "nobody notices until somebody is denied")
                .containsAll(seeded);
    }

    @Test
    void theSeededRolesMatchTheEnum() {
        List<String> declared = Arrays.stream(RoleCode.values()).map(Enum::name).toList();
        List<String> seeded = jdbc.queryForList("SELECT code FROM role", String.class);

        assertThat(seeded).containsExactlyInAnyOrderElementsOf(declared);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM role WHERE built_in = 0", Integer.class))
                .describedAs("every role the migration seeds is a built-in one")
                .isZero();
    }
}
