package com.isaac.approvalworkflowengine.workflowtemplate.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record WorkflowGraphInput(
    @NotNull @Size(min = 2) List<@Valid WorkflowNodeInput> nodes,
    @NotNull @Size(min = 1) List<@Valid WorkflowEdgeInput> edges,
    Map<String, Object> policies
) {
}
