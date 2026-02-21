package com.isaac.approvalworkflowengine.auth.repository;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTokenRevocationRepository implements TokenRevocationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTokenRevocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void revoke(String jti, Instant expiresAt) {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp expiresAtTimestamp = Timestamp.from(expiresAt);

        try {
            jdbcTemplate.update(
                "insert into auth_token_revocations (jti, revoked_at, expires_at) values (?, ?, ?)",
                jti,
                now,
                expiresAtTimestamp
            );
        } catch (DataIntegrityViolationException exception) {
            jdbcTemplate.update(
                "update auth_token_revocations set revoked_at = ?, expires_at = ? where jti = ?",
                now,
                expiresAtTimestamp,
                jti
            );
        }
    }

    @Override
    public boolean isRevoked(String jti) {
        Integer revokedCount = jdbcTemplate.queryForObject(
            "select count(*) from auth_token_revocations where jti = ? and expires_at > ?",
            Integer.class,
            jti,
            Timestamp.from(Instant.now())
        );

        return revokedCount != null && revokedCount > 0;
    }
}
