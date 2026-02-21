package com.isaac.approvalworkflowengine.auth.policy;

import java.util.Set;

public record WorkflowAccessContext(Set<String> actorRoles) {
}
