package com.isaac.approvalworkflowengine.workflowtemplate.api;

import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowAssignmentStrategy;
import jakarta.validation.constraints.NotNull;

public record WorkflowAssignmentInput(
    @NotNull WorkflowAssignmentStrategy strategy,
    String role,
    String userId,
    String expression
) {
}
