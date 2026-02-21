package com.isaac.approvalworkflowengine.auth.repository;

import com.isaac.approvalworkflowengine.auth.model.UserAccount;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserAccountRepository implements UserAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccount> findByLoginIdentifier(String loginIdentifier) {
        List<UserAccountRow> rows = jdbcTemplate.query(
            """
            select id, external_subject, email, display_name, department, employee_id, password_hash, active
            from users
            where lower(email) = lower(?)
               or external_subject = ?
            fetch first 1 rows only
            """,
            this::mapRow,
            loginIdentifier,
            loginIdentifier
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toUserAccount(rows.getFirst()));
    }

    @Override
    public Optional<UserAccount> findByExternalSubject(String externalSubject) {
        List<UserAccountRow> rows = jdbcTemplate.query(
            """
            select id, external_subject, email, display_name, department, employee_id, password_hash, active
            from users
            where external_subject = ?
            fetch first 1 rows only
            """,
            this::mapRow,
            externalSubject
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toUserAccount(rows.getFirst()));
    }

    private UserAccount toUserAccount(UserAccountRow row) {
        Set<String> roles = new LinkedHashSet<>(jdbcTemplate.queryForList(
            "select role_code from user_roles where user_id = ?",
            String.class,
            row.id
        ));

        return new UserAccount(
            row.id,
            row.externalSubject,
            row.email,
            row.displayName,
            row.department,
            row.employeeId,
            row.passwordHash,
            row.active,
            Set.copyOf(roles)
        );
    }

    private UserAccountRow mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new UserAccountRow(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("external_subject"),
            resultSet.getString("email"),
            resultSet.getString("display_name"),
            resultSet.getString("department"),
            resultSet.getString("employee_id"),
            resultSet.getString("password_hash"),
            resultSet.getBoolean("active")
        );
    }

    private record UserAccountRow(
        UUID id,
        String externalSubject,
        String email,
        String displayName,
        String department,
        String employeeId,
        String passwordHash,
        boolean active
    ) {
    }
}
