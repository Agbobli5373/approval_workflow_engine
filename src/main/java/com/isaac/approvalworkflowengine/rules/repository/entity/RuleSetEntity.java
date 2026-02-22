package com.isaac.approvalworkflowengine.rules.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "rule_sets",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_rule_sets_key_version", columnNames = {"rule_set_key", "version_no"})
    }
)
public class RuleSetEntity {

    @Id
    private UUID id;

    @Column(name = "rule_set_key", nullable = false, length = 100)
    private String ruleSetKey;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Lob
    @Column(name = "dsl_json", nullable = false)
    private String dslJson;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRuleSetKey() {
        return ruleSetKey;
    }

    public void setRuleSetKey(String ruleSetKey) {
        this.ruleSetKey = ruleSetKey;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public String getDslJson() {
        return dslJson;
    }

    public void setDslJson(String dslJson) {
        this.dslJson = dslJson;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
