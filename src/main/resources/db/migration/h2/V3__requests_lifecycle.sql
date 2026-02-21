-- E2 request lifecycle migration for H2 test/local profile.

CREATE TABLE requests (
    id UUID PRIMARY KEY,
    request_type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description CLOB,
    payload_json CLOB NOT NULL,
    amount DECIMAL(18, 2),
    currency CHAR(3),
    department VARCHAR(80),
    cost_center VARCHAR(80),
    attachments_json CLOB,
    status VARCHAR(30) NOT NULL,
    requestor_user_id UUID NOT NULL,
    workflow_version_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_requests_requestor_status ON requests (requestor_user_id, status);
CREATE INDEX idx_requests_status_created ON requests (status, created_at);
CREATE INDEX idx_requests_type_created ON requests (request_type, created_at);

CREATE TABLE request_status_transitions (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    changed_by_subject VARCHAR(128) NOT NULL,
    reason CLOB,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_request_status_transitions_request FOREIGN KEY (request_id) REFERENCES requests(id)
);

CREATE INDEX idx_request_status_transitions_request_changed
    ON request_status_transitions (request_id, changed_at);
