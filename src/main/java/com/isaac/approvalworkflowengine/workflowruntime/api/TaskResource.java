package com.isaac.approvalworkflowengine.workflowruntime.api;

import com.isaac.approvalworkflowengine.workflowruntime.model.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskResource(
    UUID id,
    UUID requestId,
    UUID workflowInstanceId,
    String stepKey,
    UUID assigneeUserId,
    String assigneeRole,
    TaskStatus status,
    Instant dueAt,
    Instant claimedAt,
    UUID claimedByUserId,
    Instant createdAt,
    Instant updatedAt
) {
}
