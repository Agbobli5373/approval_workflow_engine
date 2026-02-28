package com.isaac.approvalworkflowengine.users;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DelegationPolicyLookup {

    Optional<DelegationAuthorization> resolveDelegation(DelegationTaskContext context);

    record DelegationTaskContext(
        UUID delegateUserId,
        UUID assignedUserId,
        String assignedRole,
        String requestType,
        String department,
        Instant at
    ) {
    }

    record DelegationAuthorization(UUID delegatorUserId) {
    }
}
