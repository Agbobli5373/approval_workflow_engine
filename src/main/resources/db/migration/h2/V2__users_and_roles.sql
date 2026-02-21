-- E1 security foundation migration for H2 test/local profile.

CREATE TABLE users (
    id UUID PRIMARY KEY,
    external_subject VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(320) UNIQUE NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    department VARCHAR(80),
    employee_id VARCHAR(80),
    password_hash VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_department ON users (department);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_user_roles_user_role UNIQUE (user_id, role_code)
);

CREATE TABLE auth_token_revocations (
    jti VARCHAR(64) PRIMARY KEY,
    revoked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_auth_token_revocations_expires_at ON auth_token_revocations (expires_at);
