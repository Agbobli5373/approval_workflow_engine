package com.isaac.approvalworkflowengine.users.api;

import java.time.Instant;
import java.util.UUID;

public record DelegationResource(
    UUID id,
    UUID delegatorUserId,
    UUID delegateUserId,
    String requestType,
    String department,
    String role,
    boolean allScope,
    Instant validFrom,
    Instant validUntil,
    boolean active,
    Instant revokedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
