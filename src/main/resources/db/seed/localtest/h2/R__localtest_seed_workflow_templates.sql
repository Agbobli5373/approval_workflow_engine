-- E3 local/test seed workflow templates for H2.

MERGE INTO workflow_definitions (
    id,
    definition_key,
    name,
    request_type,
    owner_user_id,
    allow_loopback,
    created_at,
    updated_at
)
KEY(id)
VALUES (
    '11111111-0000-0000-0000-000000000001',
    'EXPENSE_DEFAULT',
    'Local Expense Approval',
    'EXPENSE',
    'a0d11f04-2e54-4b0e-bf14-7d9e05cbef4a',
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

MERGE INTO workflow_versions (
    id,
    workflow_definition_id,
    version_no,
    status,
    graph_json,
    checksum_sha256,
    activated_at,
    activated_by_user_id,
    created_at,
    updated_at
)
KEY(id)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    '11111111-0000-0000-0000-000000000001',
    1,
    'ACTIVE',
    '{"nodes":[{"id":"end","type":"END"},{"id":"manager_approval","type":"APPROVAL","assignment":{"strategy":"ROLE","role":"MANAGER"}},{"id":"start","type":"START"}],"edges":[{"from":"manager_approval","to":"end"},{"from":"start","to":"manager_approval"}]}',
    'fc2866bf748523caa273fb33abc9235ac9cf0444e92431f6d03b61546c4d4493',
    CURRENT_TIMESTAMP,
    'a0d11f04-2e54-4b0e-bf14-7d9e05cbef4a',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

DELETE FROM workflow_edges
WHERE workflow_version_id = '11111111-1111-1111-1111-111111111111';

DELETE FROM workflow_nodes
WHERE workflow_version_id = '11111111-1111-1111-1111-111111111111';

MERGE INTO workflow_nodes (
    id,
    workflow_version_id,
    node_key,
    node_type,
    config_json,
    created_at
)
KEY(id)
VALUES
    (
        '22222222-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        'start',
        'START',
        '{}',
        CURRENT_TIMESTAMP
    ),
    (
        '22222222-0000-0000-0000-000000000002',
        '11111111-1111-1111-1111-111111111111',
        'manager_approval',
        'APPROVAL',
        '{"assignment":{"strategy":"ROLE","role":"MANAGER"}}',
        CURRENT_TIMESTAMP
    ),
    (
        '22222222-0000-0000-0000-000000000003',
        '11111111-1111-1111-1111-111111111111',
        'end',
        'END',
        '{}',
        CURRENT_TIMESTAMP
    );

MERGE INTO workflow_edges (
    id,
    workflow_version_id,
    from_node_key,
    to_node_key,
    condition_json,
    created_at
)
KEY(id)
VALUES
    (
        '33333333-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        'start',
        'manager_approval',
        NULL,
        CURRENT_TIMESTAMP
    ),
    (
        '33333333-0000-0000-0000-000000000002',
        '11111111-1111-1111-1111-111111111111',
        'manager_approval',
        'end',
        NULL,
        CURRENT_TIMESTAMP
    );
