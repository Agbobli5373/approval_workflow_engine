-- E6 delegations and acting-on-behalf migration.

CREATE TABLE delegations (
    id UUID PRIMARY KEY,
    delegator_user_id UUID NOT NULL REFERENCES users (id),
    delegate_user_id UUID NOT NULL REFERENCES users (id),
    request_type VARCHAR(80),
    department VARCHAR(80),
    role_code VARCHAR(64),
    all_scope BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at TIMESTAMPTZ,
    created_by_user_id UUID NOT NULL REFERENCES users (id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_delegations_window CHECK (valid_until > valid_from),
    CONSTRAINT ck_delegations_scope CHECK (
        all_scope = TRUE OR request_type IS NOT NULL OR department IS NOT NULL OR role_code IS NOT NULL
    )
);

CREATE INDEX idx_delegations_delegator_window
    ON delegations (delegator_user_id, active, valid_from, valid_until);

CREATE INDEX idx_delegations_delegate_window
    ON delegations (delegate_user_id, active, valid_from, valid_until);

CREATE INDEX idx_delegations_delegate_active
    ON delegations (delegate_user_id, active);
