package com.isaac.approvalworkflowengine.auth.repository;

import com.isaac.approvalworkflowengine.auth.repository.entity.AuthTokenRevocationEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRevocationJpaRepository extends JpaRepository<AuthTokenRevocationEntity, String> {

    boolean existsByJtiAndExpiresAtAfter(String jti, Instant instant);
}
