package com.isaac.approvalworkflowengine.workflowtemplate;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowTemplateLookup {

    Optional<UUID> findActiveWorkflowVersionIdByRequestType(String requestType);
}
