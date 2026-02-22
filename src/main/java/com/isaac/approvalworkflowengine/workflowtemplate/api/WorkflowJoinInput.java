package com.isaac.approvalworkflowengine.workflowtemplate.api;

import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowJoinPolicy;
import jakarta.validation.constraints.NotNull;

public record WorkflowJoinInput(
    @NotNull WorkflowJoinPolicy policy,
    Integer quorum
) {
}
