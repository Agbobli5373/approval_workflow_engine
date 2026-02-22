package com.isaac.approvalworkflowengine.workflowtemplate.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_nodes")
public class WorkflowNodeEntity {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "node_key", nullable = false, length = 80)
    private String nodeKey;

    @Column(name = "node_type", nullable = false, length = 40)
    private String nodeType;

    @Lob
    @Column(name = "config_json", nullable = false)
    private String configJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowNodeEntity() {
    }

    public WorkflowNodeEntity(
        UUID id,
        UUID workflowVersionId,
        String nodeKey,
        String nodeType,
        String configJson,
        Instant createdAt
    ) {
        this.id = id;
        this.workflowVersionId = workflowVersionId;
        this.nodeKey = nodeKey;
        this.nodeType = nodeType;
        this.configJson = configJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getConfigJson() {
        return configJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
