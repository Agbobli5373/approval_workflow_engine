package com.isaac.approvalworkflowengine.workflowruntime.api;

import com.isaac.approvalworkflowengine.workflowruntime.model.TaskDecisionAction;
import java.time.Instant;
import java.util.UUID;

public record TaskDecisionResource(
    UUID id,
    UUID taskId,
    TaskDecisionAction action,
    String comment,
    UUID actedByUserId,
    UUID actedOnBehalfOfUserId,
    Instant createdAt
) {
}
