package com.isaac.approvalworkflowengine.workflowtemplate.api;

import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowNodeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowNodeInput(
    @NotBlank String id,
    @NotNull WorkflowNodeType type,
    @Valid WorkflowAssignmentInput assignment,
    @Valid WorkflowRuleRefInput ruleRef,
    @Valid WorkflowJoinInput join,
    @Valid WorkflowSlaInput sla
) {
}
