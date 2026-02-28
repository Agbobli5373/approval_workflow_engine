-- E6 delegations and acting-on-behalf migration for H2 test/local profile.

CREATE TABLE delegations (
    id UUID PRIMARY KEY,
    delegator_user_id UUID NOT NULL,
    delegate_user_id UUID NOT NULL,
    request_type VARCHAR(80),
    department VARCHAR(80),
    role_code VARCHAR(64),
    all_scope BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_by_user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_delegations_delegator FOREIGN KEY (delegator_user_id) REFERENCES users (id),
    CONSTRAINT fk_delegations_delegate FOREIGN KEY (delegate_user_id) REFERENCES users (id),
    CONSTRAINT fk_delegations_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
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
