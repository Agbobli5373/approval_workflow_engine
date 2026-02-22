package com.isaac.approvalworkflowengine.workflowruntime.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_status_transitions")
public class RuntimeRequestStatusTransitionEntity {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "from_status", nullable = false, length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "changed_by_subject", nullable = false, length = 128)
    private String changedBySubject;

    @Lob
    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected RuntimeRequestStatusTransitionEntity() {
    }

    public RuntimeRequestStatusTransitionEntity(
        UUID id,
        UUID requestId,
        String fromStatus,
        String toStatus,
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
}
