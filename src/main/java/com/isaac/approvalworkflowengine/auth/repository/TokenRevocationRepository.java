package com.isaac.approvalworkflowengine.auth.repository;

import java.time.Instant;

public interface TokenRevocationRepository {

    void revoke(String jti, Instant expiresAt);

    boolean isRevoked(String jti);
}
