package com.isaac.approvalworkflowengine.workflowruntime.model;

import java.util.Set;

public enum RuntimeRequestStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    CHANGES_REQUESTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    private static final Set<RuntimeRequestStatus> SUBMITTABLE = Set.of(DRAFT, CHANGES_REQUESTED);

    public boolean isSubmittable() {
        return SUBMITTABLE.contains(this);
    }
}
