package com.isaac.approvalworkflowengine.requests.repository.entity;

import com.isaac.approvalworkflowengine.requests.model.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_status_transitions")
public class RequestStatusTransitionEntity {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 30)
    private RequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private RequestStatus toStatus;

    @Column(name = "changed_by_subject", nullable = false, length = 128)
    private String changedBySubject;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected RequestStatusTransitionEntity() {
    }

    public RequestStatusTransitionEntity(
        UUID id,
        UUID requestId,
        RequestStatus fromStatus,
        RequestStatus toStatus,
        String changedBySubject,
        String reason,
        Instant changedAt
    ) {
        this.id = id;
        this.requestId = requestId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBySubject = changedBySubject;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public RequestStatus getFromStatus() {
        return fromStatus;
    }

    public RequestStatus getToStatus() {
        return toStatus;
    }

    public String getChangedBySubject() {
        return changedBySubject;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
