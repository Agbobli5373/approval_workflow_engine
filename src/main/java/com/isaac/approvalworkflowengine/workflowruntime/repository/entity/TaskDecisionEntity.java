package com.isaac.approvalworkflowengine.workflowruntime.repository.entity;

import com.isaac.approvalworkflowengine.workflowruntime.model.TaskDecisionAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_decisions")
public class TaskDecisionEntity {

    @Id
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private TaskDecisionAction action;

    @Lob
    @Column(name = "comment")
    private String comment;

    @Column(name = "acted_by_user_id", nullable = false)
    private UUID actedByUserId;

    @Column(name = "acted_on_behalf_of_user_id")
    private UUID actedOnBehalfOfUserId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public TaskDecisionAction getAction() {
        return action;
    }

    public void setAction(TaskDecisionAction action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public UUID getActedByUserId() {
        return actedByUserId;
    }

    public void setActedByUserId(UUID actedByUserId) {
        this.actedByUserId = actedByUserId;
    }

    public UUID getActedOnBehalfOfUserId() {
        return actedOnBehalfOfUserId;
    }

    public void setActedOnBehalfOfUserId(UUID actedOnBehalfOfUserId) {
        this.actedOnBehalfOfUserId = actedOnBehalfOfUserId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
