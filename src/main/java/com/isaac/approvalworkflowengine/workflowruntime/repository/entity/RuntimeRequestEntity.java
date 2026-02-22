package com.isaac.approvalworkflowengine.workflowruntime.repository.entity;

import com.isaac.approvalworkflowengine.workflowruntime.model.RuntimeRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class RuntimeRequestEntity {

    @Id
    private UUID id;

    @Column(name = "request_type", nullable = false, length = 80)
    private String requestType;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "department", length = 80)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RuntimeRequestStatus status;

    @Column(name = "workflow_version_id")
    private UUID workflowVersionId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RuntimeRequestEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDepartment() {
        return department;
    }

    public RuntimeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(RuntimeRequestStatus status) {
        this.status = status;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public void setWorkflowVersionId(UUID workflowVersionId) {
        this.workflowVersionId = workflowVersionId;
    }

    public long getVersion() {
        return version;
    }
}
