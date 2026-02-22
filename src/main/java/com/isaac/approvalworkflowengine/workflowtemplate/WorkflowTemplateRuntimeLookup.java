package com.isaac.approvalworkflowengine.workflowtemplate;

import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowGraphInput;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowTemplateRuntimeLookup {

    Optional<WorkflowTemplateRuntimeVersion> findRuntimeWorkflowVersion(UUID workflowVersionId);

    record WorkflowTemplateRuntimeVersion(
        UUID workflowVersionId,
        String definitionKey,
        WorkflowGraphInput graph
    ) {
    }
}
