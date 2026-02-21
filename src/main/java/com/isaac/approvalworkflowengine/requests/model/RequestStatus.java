package com.isaac.approvalworkflowengine.requests.model;

import java.util.Set;

public enum RequestStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    CHANGES_REQUESTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    private static final Set<RequestStatus> EDITABLE_STATUSES = Set.of(DRAFT, CHANGES_REQUESTED);
    private static final Set<RequestStatus> SUBMITTABLE_STATUSES = Set.of(DRAFT, CHANGES_REQUESTED);
    private static final Set<RequestStatus> CANCELLABLE_STATUSES = Set.of(DRAFT, SUBMITTED, IN_REVIEW, CHANGES_REQUESTED);

    public boolean isEditable() {
        return EDITABLE_STATUSES.contains(this);
    }

    public boolean isSubmittable() {
        return SUBMITTABLE_STATUSES.contains(this);
    }

    public boolean isCancellable() {
        return CANCELLABLE_STATUSES.contains(this);
    }
}
