package com.isaac.approvalworkflowengine.workflowtemplate.api;

import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowVersionStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkflowVersionResource(
    UUID id,
    String definitionKey,
    int versionNo,
    WorkflowVersionStatus status,
    WorkflowGraphInput graph,
    String checksumSha256,
    Instant activatedAt
) {
}
