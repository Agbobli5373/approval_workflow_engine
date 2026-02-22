package com.isaac.approvalworkflowengine.workflowtemplate.api;

import jakarta.validation.constraints.Min;

public record WorkflowSlaInput(
    @Min(1) Integer dueInHours
) {
}
