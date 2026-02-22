package com.isaac.approvalworkflowengine.workflowruntime.model;

public enum WorkflowInstanceStatus {
    ACTIVE,
    COMPLETED,
    REJECTED,
    CHANGES_REQUESTED,
    CANCELLED;

    public boolean isTerminal() {
        return this != ACTIVE;
    }
}
