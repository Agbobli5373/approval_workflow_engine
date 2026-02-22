-- E3 workflow graph nodes and edges migration.

CREATE TABLE workflow_nodes (
    id UUID PRIMARY KEY,
    workflow_version_id UUID NOT NULL REFERENCES workflow_versions (id),
    node_key VARCHAR(80) NOT NULL,
    node_type VARCHAR(40) NOT NULL,
    config_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_workflow_nodes_version_key UNIQUE (workflow_version_id, node_key)
);

CREATE TABLE workflow_edges (
    id UUID PRIMARY KEY,
    workflow_version_id UUID NOT NULL REFERENCES workflow_versions (id),
    from_node_key VARCHAR(80) NOT NULL,
    to_node_key VARCHAR(80) NOT NULL,
    condition_json JSONB,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_workflow_edges_version_from
    ON workflow_edges (workflow_version_id, from_node_key);

CREATE INDEX idx_workflow_edges_version_to
    ON workflow_edges (workflow_version_id, to_node_key);
