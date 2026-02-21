package com.isaac.approvalworkflowengine.auth.repository;

import com.isaac.approvalworkflowengine.auth.repository.entity.AuthTokenRevocationEntity;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaTokenRevocationRepository implements TokenRevocationRepository {

    private final TokenRevocationJpaRepository tokenRevocationJpaRepository;

    public JpaTokenRevocationRepository(TokenRevocationJpaRepository tokenRevocationJpaRepository) {
        this.tokenRevocationJpaRepository = tokenRevocationJpaRepository;
    }

    @Override
    @Transactional
    public void revoke(String jti, Instant expiresAt) {
        Instant now = Instant.now();
        AuthTokenRevocationEntity entity = tokenRevocationJpaRepository.findById(jti)
            .orElseGet(() -> new AuthTokenRevocationEntity(jti, now, expiresAt));

        entity.setRevokedAt(now);
        entity.setExpiresAt(expiresAt);
        tokenRevocationJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return tokenRevocationJpaRepository.existsByJtiAndExpiresAtAfter(jti, Instant.now());
    }
}
