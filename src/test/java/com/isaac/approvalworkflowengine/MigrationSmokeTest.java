package com.isaac.approvalworkflowengine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MigrationSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void postgresqlFoundationMigrationExists() {
        assertThat(new ClassPathResource("db/migration/postgresql/V1__platform_foundation.sql").exists())
            .isTrue();
    }

    @Test
    void h2FlywayMigrationCreatesFoundationTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where upper(table_name) in ('IDEMPOTENCY_KEYS', 'OUTBOX_EVENTS', 'JOB_LOCKS')
            """,
            Integer.class
        );

        assertThat(tableCount).isEqualTo(3);
    }
}
