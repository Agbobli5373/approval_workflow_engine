package com.isaac.approvalworkflowengine.requests.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyEntity {

    @Id
    private UUID id;

    @Column(name = "scope", nullable = false, length = 80)
    private String scope;

    @Column(name = "key_value", nullable = false, length = 120)
    private String keyValue;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Lob
    @Column(name = "response_json")
    private String responseJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyKeyEntity() {
    }

    public IdempotencyKeyEntity(
        UUID id,
        String scope,
        String keyValue,
        String requestHash,
        String responseJson,
        Instant createdAt
    ) {
        this.id = id;
        this.scope = scope;
        this.keyValue = keyValue;
        this.requestHash = requestHash;
        this.responseJson = responseJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getScope() {
        return scope;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
