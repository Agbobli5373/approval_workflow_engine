-- E1 security foundation migration.

CREATE TABLE users (
    id UUID PRIMARY KEY,
    external_subject VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(320) UNIQUE NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    department VARCHAR(80),
    employee_id VARCHAR(80),
    password_hash VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_users_department ON users (department);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    role_code VARCHAR(64) NOT NULL,
    CONSTRAINT uq_user_roles_user_role UNIQUE (user_id, role_code)
);

CREATE TABLE auth_token_revocations (
    jti VARCHAR(64) PRIMARY KEY,
    revoked_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auth_token_revocations_expires_at ON auth_token_revocations (expires_at);
