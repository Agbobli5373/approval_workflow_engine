package com.isaac.approvalworkflowengine.auth.policy;

import java.util.Set;
import java.util.UUID;

public record TaskAccessContext(
    UUID actorUserId,
    Set<String> actorRoles,
    UUID assignedUserId,
    String assignedRole,
    String actorDepartment,
    String taskDepartment
) {
}
