package com.isaac.approvalworkflowengine.requests.service;

import com.isaac.approvalworkflowengine.workflowtemplate.WorkflowTemplateLookup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DatabaseBackedRequestWorkflowVersionResolver implements RequestWorkflowVersionResolver {

    private final WorkflowTemplateLookup workflowTemplateLookup;

    public DatabaseBackedRequestWorkflowVersionResolver(WorkflowTemplateLookup workflowTemplateLookup) {
        this.workflowTemplateLookup = workflowTemplateLookup;
    }

    @Override
    public Optional<UUID> resolveActiveWorkflowVersionId(String requestType) {
        return workflowTemplateLookup.findActiveWorkflowVersionIdByRequestType(requestType);
    }
}
