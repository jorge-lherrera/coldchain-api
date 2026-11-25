package com.coldchain.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TriageSchemaIT {

    private static final List<String> TABLES = List.of("event_publication", "excursion_alert", "inbox_event");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theSchemaIsMigratedFromNothing() {
        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_name <> 'flyway_schema_history'
                ORDER BY table_name""", String.class);

        assertThat(tables)
                .describedAs("Flyway owns the schema, and an empty PostgreSQL ends up with exactly these")
                .containsExactlyElementsOf(TABLES);
    }

    @Test
    void anAlertCannotCarryHalfATriage() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO excursion_alert (id, excursion_id, shipment_id, organization_id, kind, status,
                                             source, created_at, updated_at)
                VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), gen_random_uuid(),
                        'ABOVE_MAX', 'OPEN', 'RULES', now(), now())"""))
                .describedAs("a source without a severity, a cause and a time is a triage nobody can read")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void anInboxEventCannotBeProcessedWithoutSayingWhen() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO inbox_event (event_id, type, excursion_id, occurred_at, received_at, status)
                VALUES (gen_random_uuid(), 'ExcursionClosed', gen_random_uuid(), now(), now(), 'PROCESSED')"""))
                .describedAs("the inbox decides what Kafka redelivers, so a processed event says when it was")
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
