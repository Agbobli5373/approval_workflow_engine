package com.isaac.approvalworkflowengine.platform;

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
    void postgresqlRequestLifecycleMigrationExists() {
        assertThat(new ClassPathResource("db/migration/postgresql/V3__requests_lifecycle.sql").exists())
            .isTrue();
    }

    @Test
    void h2FlywayMigrationCreatesPlatformAuthAndRequestTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where upper(table_schema) = 'PUBLIC'
              and upper(table_type) = 'BASE TABLE'
              and upper(table_name) in (
                'IDEMPOTENCY_KEYS',
                'OUTBOX_EVENTS',
                'JOB_LOCKS',
                'USERS',
                'USER_ROLES',
                'AUTH_TOKEN_REVOCATIONS',
                'REQUESTS',
                'REQUEST_STATUS_TRANSITIONS'
            )
            """,
            Integer.class
        );

        assertThat(tableCount).isEqualTo(8);
    }

    @Test
    void localTestSeedUsersArePresent() {
        Integer userCount = jdbcTemplate.queryForObject(
            "select count(*) from users where external_subject in ('admin', 'requestor', 'approver')",
            Integer.class
        );

        assertThat(userCount).isEqualTo(3);
    }
}
