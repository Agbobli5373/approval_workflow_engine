-- E1 local/test seed users.

MERGE INTO users (id, external_subject, email, display_name, department, employee_id, password_hash, active, created_at, updated_at)
KEY(id)
VALUES
    ('a0d11f04-2e54-4b0e-bf14-7d9e05cbef4a', 'admin', 'admin@local.approval', 'Admin User', 'Platform', 'EMP-ADMIN', '${seed_admin_password_hash}', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('5ad15712-2c98-4b9d-8f1d-6baf6a4f6d78', 'requestor', 'requestor@local.approval', 'Requestor User', 'Finance', 'EMP-REQ', '${seed_requestor_password_hash}', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6f6ea9ed-4c1a-4302-a7ab-2c9f4bf4384f', 'approver', 'approver@local.approval', 'Approver User', 'Finance', 'EMP-APR', '${seed_approver_password_hash}', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO user_roles (id, user_id, role_code)
KEY(id)
VALUES
    ('ca80d716-878b-4c6c-aa8c-4f489f84d8d5', 'a0d11f04-2e54-4b0e-bf14-7d9e05cbef4a', 'WORKFLOW_ADMIN'),
    ('f5478917-6842-444f-9918-77cd073fa2b0', '5ad15712-2c98-4b9d-8f1d-6baf6a4f6d78', 'REQUESTOR'),
    ('82f67460-a7d6-4a13-bd06-badca4f7f38e', '6f6ea9ed-4c1a-4302-a7ab-2c9f4bf4384f', 'APPROVER');
