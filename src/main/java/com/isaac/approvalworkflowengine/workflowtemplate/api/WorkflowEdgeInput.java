package com.isaac.approvalworkflowengine.workflowtemplate.api;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record WorkflowEdgeInput(
    @NotBlank String from,
    @NotBlank String to,
    Map<String, Object> condition
) {
}
