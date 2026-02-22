package com.isaac.approvalworkflowengine.workflowtemplate.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record WorkflowVersionInput(
    @NotNull @Valid WorkflowGraphInput graph
) {
}
