package com.isaac.approvalworkflowengine.workflowtemplate.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_edges")
public class WorkflowEdgeEntity {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "from_node_key", nullable = false, length = 80)
    private String fromNodeKey;

    @Column(name = "to_node_key", nullable = false, length = 80)
    private String toNodeKey;

    @Lob
    @Column(name = "condition_json")
    private String conditionJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowEdgeEntity() {
    }

    public WorkflowEdgeEntity(
        UUID id,
        UUID workflowVersionId,
        String fromNodeKey,
        String toNodeKey,
        String conditionJson,
        Instant createdAt
    ) {
        this.id = id;
        this.workflowVersionId = workflowVersionId;
        this.fromNodeKey = fromNodeKey;
        this.toNodeKey = toNodeKey;
        this.conditionJson = conditionJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public String getFromNodeKey() {
        return fromNodeKey;
    }

    public String getToNodeKey() {
        return toNodeKey;
    }

    public String getConditionJson() {
        return conditionJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
