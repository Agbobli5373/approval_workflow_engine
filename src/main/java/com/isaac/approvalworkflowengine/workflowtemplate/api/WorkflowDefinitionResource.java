package com.isaac.approvalworkflowengine.workflowtemplate.api;

import java.util.UUID;

public record WorkflowDefinitionResource(
    UUID id,
    String definitionKey,
    String name,
    String requestType,
    boolean allowLoopback
) {
}
