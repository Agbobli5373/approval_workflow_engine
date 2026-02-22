-- E3 workflow definitions and versions migration for H2 test/local profile.

CREATE TABLE workflow_definitions (
    id UUID PRIMARY KEY,
    definition_key VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    request_type VARCHAR(80) NOT NULL,
    owner_user_id UUID NOT NULL,
    allow_loopback BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_workflow_definitions_key UNIQUE (definition_key),
    CONSTRAINT uq_workflow_definitions_request_type UNIQUE (request_type)
);

CREATE INDEX idx_workflow_definitions_request_type
    ON workflow_definitions (request_type);

CREATE TABLE workflow_versions (
    id UUID PRIMARY KEY,
    workflow_definition_id UUID NOT NULL,
    version_no INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    graph_json CLOB NOT NULL,
    checksum_sha256 CHAR(64),
    activated_at TIMESTAMP WITH TIME ZONE,
    activated_by_user_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_workflow_versions_definition FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions (id),
    CONSTRAINT uq_workflow_versions_definition_version UNIQUE (workflow_definition_id, version_no),
    CONSTRAINT ck_workflow_versions_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED'))
);

CREATE INDEX idx_workflow_versions_definition_status
    ON workflow_versions (workflow_definition_id, status);

-- H2 does not support PostgreSQL-style partial indexes.
-- One-active-per-definition is enforced by service logic in tests/local.
