package com.isaac.approvalworkflowengine.auth.policy;

import java.util.Set;
import java.util.UUID;

public record RequestAccessContext(
    UUID actorUserId,
    Set<String> actorRoles,
    UUID requestorUserId,
    String requestStatus
) {
}
