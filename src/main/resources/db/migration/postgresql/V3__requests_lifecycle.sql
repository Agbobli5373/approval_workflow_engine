-- E2 request lifecycle migration.

CREATE TABLE requests (
    id UUID PRIMARY KEY,
    request_type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    payload_json JSONB NOT NULL,
    amount NUMERIC(18, 2),
    currency CHAR(3),
    department VARCHAR(80),
    cost_center VARCHAR(80),
    attachments_json JSONB,
    status VARCHAR(30) NOT NULL,
    requestor_user_id UUID NOT NULL,
    workflow_version_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_requests_requestor_status ON requests (requestor_user_id, status);
CREATE INDEX idx_requests_status_created ON requests (status, created_at);
CREATE INDEX idx_requests_type_created ON requests (request_type, created_at);

CREATE TABLE request_status_transitions (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES requests(id),
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    changed_by_subject VARCHAR(128) NOT NULL,
    reason TEXT,
    changed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_request_status_transitions_request_changed
    ON request_status_transitions (request_id, changed_at);
