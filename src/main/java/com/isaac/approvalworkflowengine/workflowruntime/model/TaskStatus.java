package com.isaac.approvalworkflowengine.workflowruntime.model;

import java.util.EnumSet;

public enum TaskStatus {
    PENDING,
    CLAIMED,
    APPROVED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    SKIPPED;

    private static final EnumSet<TaskStatus> ACTIVE = EnumSet.of(PENDING, CLAIMED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
