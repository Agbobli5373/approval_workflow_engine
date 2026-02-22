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
    void postgresqlWorkflowTemplateMigrationsExist() {
        assertThat(new ClassPathResource("db/migration/postgresql/V4__workflow_definitions_and_versions.sql").exists())
            .isTrue();
        assertThat(new ClassPathResource("db/migration/postgresql/V5__workflow_graph_nodes_edges.sql").exists())
            .isTrue();
    }

    @Test
    void postgresqlRulesMigrationExists() {
        assertThat(new ClassPathResource("db/migration/postgresql/V6__rulesets.sql").exists())
            .isTrue();
    }

    @Test
    void h2RulesMigrationExists() {
        assertThat(new ClassPathResource("db/migration/h2/V6__rulesets.sql").exists())
            .isTrue();
    }

    @Test
    void postgresqlRuntimeMigrationExists() {
        assertThat(new ClassPathResource("db/migration/postgresql/V7__workflow_runtime_instances_tasks.sql").exists())
            .isTrue();
    }

    @Test
    void h2RuntimeMigrationExists() {
        assertThat(new ClassPathResource("db/migration/h2/V7__workflow_runtime_instances_tasks.sql").exists())
            .isTrue();
    }

    @Test
    void h2FlywayMigrationCreatesPlatformAuthRequestWorkflowTemplateRulesAndRuntimeTables() {
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
                'REQUEST_STATUS_TRANSITIONS',
                'WORKFLOW_DEFINITIONS',
                'WORKFLOW_VERSIONS',
                'WORKFLOW_NODES',
                'WORKFLOW_EDGES',
                'RULE_SETS',
                'WORKFLOW_INSTANCES',
                'TASKS',
                'TASK_DECISIONS'
            )
            """,
            Integer.class
        );

        assertThat(tableCount).isEqualTo(16);
    }

    @Test
    void localTestSeedUsersArePresent() {
        Integer userCount = jdbcTemplate.queryForObject(
            "select count(*) from users where external_subject in ('admin', 'requestor', 'approver')",
            Integer.class
        );

        assertThat(userCount).isEqualTo(3);
    }

    @Test
    void localTestSeedActiveExpenseWorkflowTemplateIsPresent() {
        Integer definitionCount = jdbcTemplate.queryForObject(
            "select count(*) from workflow_definitions where definition_key = 'EXPENSE_DEFAULT' and request_type = 'EXPENSE'",
            Integer.class
        );

        Integer activeVersionCount = jdbcTemplate.queryForObject(
            """
            select count(*)
            from workflow_versions
            where workflow_definition_id = '11111111-0000-0000-0000-000000000001'
              and status = 'ACTIVE'
            """,
            Integer.class
        );

        assertThat(definitionCount).isEqualTo(1);
        assertThat(activeVersionCount).isEqualTo(1);
    }
}
