package com.isaac.approvalworkflowengine.workflowtemplate.api;

public record WorkflowRuleRefInput(
    String ruleSetKey,
    int version
) {
}
