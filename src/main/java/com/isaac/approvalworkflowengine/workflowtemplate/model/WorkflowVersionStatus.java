package com.isaac.approvalworkflowengine.workflowtemplate.model;

public enum WorkflowVersionStatus {
    DRAFT,
    ACTIVE,
    RETIRED;

    public boolean isMutable() {
        return this == DRAFT;
    }
}
