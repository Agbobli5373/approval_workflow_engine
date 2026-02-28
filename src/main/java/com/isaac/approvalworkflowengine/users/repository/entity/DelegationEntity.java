package com.isaac.approvalworkflowengine.users.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delegations")
public class DelegationEntity {

    @Id
    private UUID id;

    @Column(name = "delegator_user_id", nullable = false)
    private UUID delegatorUserId;

    @Column(name = "delegate_user_id", nullable = false)
    private UUID delegateUserId;

    @Column(name = "request_type", length = 80)
    private String requestType;

    @Column(name = "department", length = 80)
    private String department;

    @Column(name = "role_code", length = 64)
    private String roleCode;

    @Column(name = "all_scope", nullable = false)
    private boolean allScope;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

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

    public UUID getDelegatorUserId() {
        return delegatorUserId;
    }

    public void setDelegatorUserId(UUID delegatorUserId) {
        this.delegatorUserId = delegatorUserId;
    }

    public UUID getDelegateUserId() {
        return delegateUserId;
    }

    public void setDelegateUserId(UUID delegateUserId) {
        this.delegateUserId = delegateUserId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public boolean isAllScope() {
        return allScope;
    }

    public void setAllScope(boolean allScope) {
        this.allScope = allScope;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
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
