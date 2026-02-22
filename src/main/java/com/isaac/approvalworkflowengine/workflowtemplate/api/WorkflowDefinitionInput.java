package com.isaac.approvalworkflowengine.workflowtemplate.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkflowDefinitionInput(
    @NotBlank @Size(max = 100) String definitionKey,
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 80) String requestType,
    Boolean allowLoopback
) {
}
