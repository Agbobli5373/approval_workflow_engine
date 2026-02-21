-- E0 foundational migration.
-- Domain-specific migrations begin at V2.

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    scope VARCHAR(80) NOT NULL,
    key_value VARCHAR(120) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_json JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_scope_key UNIQUE (scope, key_value)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(60) NOT NULL,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status_next_attempt ON outbox_events (status, next_attempt_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);

CREATE TABLE job_locks (
    lock_name VARCHAR(120) PRIMARY KEY,
    locked_until TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
