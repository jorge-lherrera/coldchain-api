package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class SchemaStandardIT {

    private static final String OURS = "UPPER(TABLE_NAME) NOT LIKE 'FLYWAY%'";

    private static final List<String> AUDIT_COLUMNS =
            List.of("CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY");

    private static final List<String> LINK_TABLES =
            List.of("USER_ROLE", "ROLE_SCOPE", "API_CLIENT_SCOPE");

    private static final Map<String, String> MODULE_OF = Map.ofEntries(
            Map.entry("ORGANIZATION", "identity"),
            Map.entry("APP_USER", "identity"),
            Map.entry("ROLE", "identity"),
            Map.entry("ROLE_SCOPE", "identity"),
            Map.entry("USER_ROLE", "identity"),
            Map.entry("API_CLIENT", "identity"),
            Map.entry("API_CLIENT_SCOPE", "identity"),
            Map.entry("REFRESH_TOKEN", "identity"),
            Map.entry("AUDIT_ENTRY", "identity"),
            Map.entry("STORAGE_PROFILE", "catalog"),
            Map.entry("PRODUCT", "catalog"),
            Map.entry("SITE", "catalog"),
            Map.entry("SHIPMENT", "shipment"),
            Map.entry("SHIPMENT_LINE", "shipment"),
            Map.entry("SHIPMENT_PARTICIPANT", "shipment"),
            Map.entry("CUSTODY_EVENT", "shipment"),
            Map.entry("HANDOFF_REQUEST", "shipment"),
            Map.entry("SENSOR_DEVICE", "telemetry"),
            Map.entry("DEVICE_ASSIGNMENT", "telemetry"),
            Map.entry("READING_BATCH", "telemetry"),
            Map.entry("TEMPERATURE_READING", "telemetry"),
            Map.entry("EXCURSION", "telemetry"),
            Map.entry("COMPLIANCE_CERTIFICATE", "compliance"),
            Map.entry("CERTIFICATE_FINDING", "compliance"));

    private static final List<String> TENANT_TABLES =
            List.of("APP_USER", "API_CLIENT", "REFRESH_TOKEN", "AUDIT_ENTRY");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void n1_1_everyTableHasAPrimaryKey() {
        List<String> without = jdbc.queryForList("""
                SELECT t.table_name
                FROM user_tables t
                WHERE %s
                  AND NOT EXISTS (SELECT 1 FROM user_constraints c
                                  WHERE c.table_name = t.table_name AND c.constraint_type = 'P')
                """.formatted(OURS), String.class);

        assertThat(tables()).describedAs("the schema was migrated").isNotEmpty();
        assertThat(without).describedAs("a table with no primary key has no identity").isEmpty();
    }

    @Test
    void n1_2_uuidKeysAreStoredAsRaw16() {
        List<Map<String, Object>> wrong = jdbc.queryForList("""
                SELECT c.table_name, c.column_name, c.data_type, c.data_length
                FROM user_tab_columns c
                WHERE %s
                  AND (c.column_name = 'ID' OR SUBSTR(c.column_name, -3) = '_ID')
                  AND EXISTS (
                        SELECT 1
                        FROM user_cons_columns cc
                        JOIN user_constraints k ON k.constraint_name = cc.constraint_name
                        WHERE cc.table_name = c.table_name
                          AND cc.column_name = c.column_name
                          AND k.constraint_type IN ('P', 'R'))
                  AND NOT (c.data_type = 'RAW' AND c.data_length = 16)
                """.formatted(OURS));

        assertThat(wrong)
                .describedAs("an identifier is sixteen bytes of RAW, never a string and never a number")
                .isEmpty();
    }

    @Test
    void n1_3_linkTablesAreKeyedByTheirPair() {
        assertThat(LINK_TABLES).allSatisfy(table -> assertThat(primaryKeyColumnsOf(table))
                .describedAs("the primary key of %s is the pair it links, not a synthetic id", table)
                .hasSize(2)
                .doesNotContain("ID"));
    }

    @Test
    void n2_1_intraModuleReferencesCarryAForeignKey() {
        List<String> missing = new ArrayList<>();
        List<String> crossing = new ArrayList<>();
        for (Map<String, Object> reference : referenceColumns()) {
            String table = (String) reference.get("TABLE_NAME");
            String column = (String) reference.get("COLUMN_NAME");
            String parent = column.substring(0, column.length() - 3);
            if (!MODULE_OF.containsKey(parent)) {
                continue;
            }
            boolean declared = ((Number) reference.get("FOREIGN_KEYS")).intValue() > 0;
            boolean sameModule = MODULE_OF.get(table).equals(MODULE_OF.get(parent));
            if (sameModule && !declared) {
                missing.add(table + "." + column);
            }
            if (!sameModule && declared && !parent.equals("ORGANIZATION")) {
                crossing.add(table + "." + column);
            }
        }

        assertThat(missing)
                .describedAs("a reference inside the module is declared, so the database keeps it true")
                .isEmpty();
        assertThat(crossing)
                .describedAs("a reference that crosses a module carries no foreign key, or the two "
                        + "modules can never be deployed apart; tenancy is the one exception")
                .isEmpty();
    }

    @Test
    void n2_3_everyForeignKeyIsIndexed() {
        List<String> unindexed = jdbc.queryForList("""
                SELECT k.constraint_name
                FROM user_constraints k
                JOIN user_cons_columns cc
                  ON cc.constraint_name = k.constraint_name AND cc.position = 1
                WHERE k.constraint_type = 'R'
                  AND NOT EXISTS (
                        SELECT 1 FROM user_ind_columns i
                        WHERE i.table_name = cc.table_name
                          AND i.column_name = cc.column_name
                          AND i.column_position = 1)
                """, String.class);

        assertThat(foreignKeys()).describedAs("there are foreign keys to check").isNotEmpty();
        assertThat(unindexed)
                .describedAs("without an index on the child column Oracle scans or locks it when the "
                        + "parent changes")
                .isEmpty();
    }

    @Test
    void n3_1_tenantColumnIsEnforced() {
        assertThat(TENANT_TABLES).allSatisfy(table -> assertThat(jdbc.queryForObject("""
                SELECT nullable FROM user_tab_columns
                WHERE table_name = ? AND column_name = 'ORGANIZATION_ID'
                """, String.class, table))
                .describedAs("%s holds organization data, so its tenant cannot be null", table)
                .isEqualTo("N"));
    }

    @Test
    void n3_2_tenantColumnLeadsAnIndex() {
        assertThat(TENANT_TABLES).allSatisfy(table -> assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_ind_columns
                WHERE table_name = ? AND column_name = 'ORGANIZATION_ID' AND column_position = 1
                """, Integer.class, table))
                .describedAs("every query on %s filters by tenant, so the tenant leads an index", table)
                .isPositive());
    }

    @Test
    void n4_3_timestampsCarryTimeZone() {
        List<String> naive = jdbc.queryForList("""
                SELECT table_name || '.' || column_name
                FROM user_tab_columns
                WHERE %s
                  AND data_type LIKE 'TIMESTAMP%%'
                  AND data_type NOT LIKE '%%WITH TIME ZONE'
                """.formatted(OURS), String.class);

        assertThat(naive)
                .describedAs("a cold chain crosses borders: an instant without a zone is ambiguous")
                .isEmpty();
    }

    @Test
    void n4_4_flagsAreNumberOneWithACheck() {
        List<String> flags = jdbc.queryForList("""
                SELECT table_name || '.' || column_name
                FROM user_tab_columns
                WHERE %s AND data_type = 'NUMBER' AND data_precision = 1
                """.formatted(OURS), String.class);

        assertThat(flags).describedAs("there is a flag to check").isNotEmpty();
        assertThat(jdbc.queryForList("""
                SELECT table_name || '.' || column_name FROM user_tab_columns
                WHERE %s AND data_type = 'BOOLEAN'
                """.formatted(OURS), String.class))
                .describedAs("the native 23ai BOOLEAN breaks a function-based index built on it")
                .isEmpty();
        assertThat(flags).allSatisfy(flag -> {
            String column = flag.substring(flag.indexOf('.') + 1);
            assertThat(checkConditions()).describedAs("%s must be constrained to 0 or 1", flag)
                    .anyMatch(condition -> condition.contains(column + "IN(0,1)"));
        });
    }

    @Test
    void n4_1_measurableQuantitiesAreExactDecimals() {
        List<String> approximate = jdbc.queryForList("""
                SELECT table_name || '.' || column_name
                FROM user_tab_columns
                WHERE %s AND data_type IN ('BINARY_DOUBLE', 'BINARY_FLOAT', 'FLOAT')
                """.formatted(OURS), String.class);
        List<String> temperatures = jdbc.queryForList("""
                SELECT table_name || '.' || column_name
                FROM user_tab_columns
                WHERE %s AND column_name LIKE '%%CELSIUS'
                  AND (data_type <> 'NUMBER' OR data_precision <> 5 OR data_scale <> 2)
                """.formatted(OURS), String.class);

        assertThat(approximate)
                .describedAs("a temperature that is summed and compared cannot be approximate")
                .isEmpty();
        assertThat(temperatures)
                .describedAs("every temperature is NUMBER(5,2), so two readings compare exactly")
                .isEmpty();
    }

    @Test
    void n6_4_everyTableIsAuditable() {
        assertThat(tables()).allSatisfy(table -> {
            assertThat(columnsOf(table))
                    .describedAs("%s must carry the four audit columns", table)
                    .containsAll(AUDIT_COLUMNS);
            assertThat(jdbc.queryForList("""
                    SELECT column_name FROM user_tab_columns
                    WHERE table_name = ? AND column_name IN ('CREATED_AT', 'CREATED_BY') AND nullable = 'Y'
                    """, String.class, table))
                    .describedAs("a row that does not know who created it is not auditable")
                    .isEmpty();
        });
    }

    @Test
    void n6_1_softDeleteHasOneName() {
        List<String> otherNames = jdbc.queryForList("""
                SELECT table_name || '.' || column_name
                FROM user_tab_columns
                WHERE %s
                  AND (column_name IN ('IS_DELETED', 'DELETED', 'ACTIVE', 'IS_ACTIVE', 'REMOVED_AT')
                    OR SUBSTR(column_name, 1, 8) = 'DELETED_')
                  AND column_name <> 'DELETED_AT'
                """.formatted(OURS), String.class);
        List<String> softDeleted = jdbc.queryForList("""
                SELECT table_name FROM user_tab_columns WHERE %s AND column_name = 'DELETED_AT'
                """.formatted(OURS), String.class);

        assertThat(softDeleted).describedAs("there is a soft-deleted table to check").isNotEmpty();
        assertThat(otherNames)
                .describedAs("one question, one column: DELETED_AT, and NULL means alive")
                .isEmpty();
        assertThat(jdbc.queryForList("""
                SELECT table_name FROM user_tab_columns
                WHERE %s AND column_name = 'DELETED_AT' AND data_type NOT LIKE '%%WITH TIME ZONE'
                """.formatted(OURS), String.class))
                .describedAs("it answers when, not only whether")
                .isEmpty();
    }

    @Test
    void n7_3_conditionalUniquenessIsAFunctionBasedIndex() {
        List<String> plainUniques = jdbc.queryForList("""
                SELECT i.index_name
                FROM user_indexes i
                WHERE i.uniqueness = 'UNIQUE'
                  AND UPPER(i.table_name) NOT LIKE 'FLYWAY%%'
                  AND i.index_type NOT LIKE 'FUNCTION%%'
                  AND NOT EXISTS (SELECT 1 FROM user_constraints k
                                  WHERE k.index_name = i.index_name AND k.constraint_type = 'P')
                  AND EXISTS (SELECT 1 FROM user_tab_columns c
                              WHERE c.table_name = i.table_name AND c.column_name = 'DELETED_AT')
                """.formatted(), String.class);

        assertThat(jdbc.queryForList("""
                SELECT index_name FROM user_indexes
                WHERE uniqueness = 'UNIQUE' AND index_type LIKE 'FUNCTION%'
                """, String.class))
                .describedAs("there is a conditional uniqueness to check")
                .isNotEmpty();
        assertThat(plainUniques)
                .describedAs("a plain unique index over a soft-deleted table forbids reusing the code "
                        + "of a row nobody can see any more")
                .isEmpty();
    }

    @Test
    void n8_1_mandatoryTextRejectsTheEmptyString() {
        List<String> mandatoryText = jdbc.queryForList("""
                SELECT table_name || '.' || column_name
                FROM user_tab_columns
                WHERE %s AND data_type LIKE 'VARCHAR2%%' AND nullable = 'N'
                  AND column_name NOT IN ('CREATED_BY', 'UPDATED_BY')
                """.formatted(OURS), String.class);

        assertThat(mandatoryText).describedAs("there is mandatory text to check").isNotEmpty();
        assertThat(mandatoryText).allSatisfy(column -> {
            String name = column.substring(column.indexOf('.') + 1);
            assertThat(checkConditions())
                    .describedAs("in Oracle a blank is not the empty string, so %s is protected either "
                            + "by TRIM(...) IS NOT NULL or by the closed set it belongs to", column)
                    .anyMatch(condition -> condition.contains("TRIM(" + name + ")ISNOTNULL")
                            || condition.contains(name + "IN("));
        });
    }

    @Test
    void n8_2_closedSetsAreChecks() {
        List<String> stateColumns = jdbc.queryForList("""
                SELECT table_name || '.' || column_name
                FROM user_tab_columns
                WHERE %s AND column_name IN ('STATUS', 'KIND', 'ACTOR_TYPE')
                """.formatted(OURS), String.class);

        assertThat(stateColumns).describedAs("there is a closed set to check").isNotEmpty();
        assertThat(stateColumns).allSatisfy(column -> {
            String name = column.substring(column.indexOf('.') + 1);
            assertThat(checkConditions())
                    .describedAs("%s is a closed set, so the database refuses anything outside it", column)
                    .anyMatch(condition -> condition.contains(name + "IN("));
        });
    }

    @Test
    void n8_3_businessUniquenessIsEnforcedByTheDatabase() {
        List<String> uniqueOver = jdbc.queryForList("""
                SELECT i.table_name || '.' || LISTAGG(c.column_name, '+')
                         WITHIN GROUP (ORDER BY c.column_position)
                FROM user_indexes i
                JOIN user_ind_columns c ON c.index_name = i.index_name
                WHERE i.uniqueness = 'UNIQUE' AND UPPER(i.table_name) NOT LIKE 'FLYWAY%'
                GROUP BY i.index_name, i.table_name
                """, String.class);

        assertThat(uniqueOver)
                .describedAs("the code validates to explain, the database validates to be true")
                .contains("ORGANIZATION.TAX_ID", "API_CLIENT.CLIENT_ID", "REFRESH_TOKEN.TOKEN_HASH",
                        "ROLE.CODE");
        assertThat(jdbc.queryForList("""
                SELECT index_name FROM user_indexes
                WHERE table_name = 'APP_USER' AND uniqueness = 'UNIQUE' AND index_type LIKE 'FUNCTION%'
                """, String.class))
                .describedAs("an email is unique regardless of its casing, which only a function-based "
                        + "index can say in Oracle")
                .isNotEmpty();
    }

    @Test
    void n9_1_noSystemGeneratedConstraintNames() {
        List<String> generated = jdbc.queryForList("""
                SELECT constraint_name
                FROM user_constraints
                WHERE %s
                  AND constraint_name LIKE 'SYS\\_%%' ESCAPE '\\'
                  AND (constraint_type <> 'C' OR search_condition_vc NOT LIKE '%%IS NOT NULL')
                """.formatted(OURS), String.class);

        assertThat(generated)
                .describedAs("a constraint nobody named is one nobody can talk about in an incident")
                .isEmpty();
    }

    @Test
    void n9_1_everyNamedConstraintFollowsItsPrefix() {
        List<String> misnamed = jdbc.queryForList("""
                SELECT constraint_name || ' (' || constraint_type || ')'
                FROM user_constraints
                WHERE %s
                  AND constraint_name NOT LIKE 'SYS\\_%%' ESCAPE '\\'
                  AND ((constraint_type = 'P' AND constraint_name NOT LIKE 'PK\\_%%' ESCAPE '\\')
                    OR (constraint_type = 'R' AND constraint_name NOT LIKE 'FK\\_%%' ESCAPE '\\')
                    OR (constraint_type = 'U' AND constraint_name NOT LIKE 'UQ\\_%%' ESCAPE '\\')
                    OR (constraint_type = 'C' AND constraint_name NOT LIKE 'CK\\_%%' ESCAPE '\\'))
                """.formatted(OURS), String.class);

        assertThat(misnamed)
                .describedAs("the prefix says what the constraint is before anybody opens it")
                .isEmpty();
    }

    private List<Map<String, Object>> referenceColumns() {
        return jdbc.queryForList("""
                SELECT c.table_name, c.column_name,
                       (SELECT COUNT(*)
                        FROM user_cons_columns cc
                        JOIN user_constraints k ON k.constraint_name = cc.constraint_name
                        WHERE cc.table_name = c.table_name
                          AND cc.column_name = c.column_name
                          AND k.constraint_type = 'R') AS foreign_keys
                FROM user_tab_columns c
                WHERE %s
                  AND c.data_type = 'RAW'
                  AND c.column_name <> 'ID'
                  AND c.column_name NOT IN ('CREATED_BY', 'UPDATED_BY')
                """.formatted(OURS));
    }

    private List<String> tables() {
        return jdbc.queryForList(
                "SELECT table_name FROM user_tables WHERE " + OURS, String.class);
    }

    private List<String> foreignKeys() {
        return jdbc.queryForList(
                "SELECT constraint_name FROM user_constraints WHERE constraint_type = 'R'", String.class);
    }

    private List<String> columnsOf(String table) {
        return jdbc.queryForList(
                "SELECT column_name FROM user_tab_columns WHERE table_name = ?", String.class, table);
    }

    private List<String> primaryKeyColumnsOf(String table) {
        return jdbc.queryForList("""
                SELECT cc.column_name
                FROM user_constraints c
                JOIN user_cons_columns cc ON cc.constraint_name = c.constraint_name
                WHERE c.table_name = ? AND c.constraint_type = 'P'
                ORDER BY cc.position
                """, String.class, table);
    }

    private static String compact(String condition) {
        StringBuilder compacted = new StringBuilder(condition.length());
        for (char character : condition.toCharArray()) {
            if (!Character.isWhitespace(character) && character != '"') {
                compacted.append(character);
            }
        }
        return compacted.toString();
    }

    private List<String> checkConditions() {
        return jdbc.queryForList("""
                SELECT search_condition_vc FROM user_constraints WHERE constraint_type = 'C'
                """, String.class).stream()
                .filter(condition -> condition != null)
                .map(SchemaStandardIT::compact)
                .collect(Collectors.toList());
    }
}
