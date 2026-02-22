package com.isaac.approvalworkflowengine.workflowruntime.repository.entity;

import com.isaac.approvalworkflowengine.workflowruntime.model.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    private UUID id;

    @Column(name = "workflow_instance_id", nullable = false)
    private UUID workflowInstanceId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "step_key", nullable = false, length = 80)
    private String stepKey;

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(name = "assignee_role", length = 64)
    private String assigneeRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by_user_id")
    private UUID claimedByUserId;

    @Column(name = "join_policy", length = 20)
    private String joinPolicy;

    @Column(name = "quorum_required")
    private Integer quorumRequired;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(UUID workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public String getStepKey() {
        return stepKey;
    }

    public void setStepKey(String stepKey) {
        this.stepKey = stepKey;
    }

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(UUID assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getAssigneeRole() {
        return assigneeRole;
    }

    public void setAssigneeRole(String assigneeRole) {
        this.assigneeRole = assigneeRole;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public UUID getClaimedByUserId() {
        return claimedByUserId;
    }

    public void setClaimedByUserId(UUID claimedByUserId) {
        this.claimedByUserId = claimedByUserId;
    }

    public String getJoinPolicy() {
        return joinPolicy;
    }

    public void setJoinPolicy(String joinPolicy) {
        this.joinPolicy = joinPolicy;
    }

    public Integer getQuorumRequired() {
        return quorumRequired;
    }

    public void setQuorumRequired(Integer quorumRequired) {
        this.quorumRequired = quorumRequired;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
