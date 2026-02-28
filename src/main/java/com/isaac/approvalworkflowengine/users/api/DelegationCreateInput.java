package com.isaac.approvalworkflowengine.users.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record DelegationCreateInput(
    UUID delegatorUserId,
    @NotNull UUID delegateUserId,
    String requestType,
    String department,
    String role,
    Boolean allScope,
    @NotNull Instant validFrom,
    @NotNull @Future Instant validUntil
) {
}
