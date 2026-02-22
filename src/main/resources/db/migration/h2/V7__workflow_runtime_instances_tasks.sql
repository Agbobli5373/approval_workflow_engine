-- E5 workflow runtime instances, tasks, and decisions migration for H2 test/local profile.

CREATE TABLE workflow_instances (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    workflow_version_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_step_keys CLOB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_workflow_instances_request FOREIGN KEY (request_id) REFERENCES requests (id),
    CONSTRAINT fk_workflow_instances_version FOREIGN KEY (workflow_version_id) REFERENCES workflow_versions (id),
    CONSTRAINT uq_workflow_instances_request UNIQUE (request_id),
    CONSTRAINT ck_workflow_instances_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'REJECTED', 'CHANGES_REQUESTED', 'CANCELLED'))
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    workflow_instance_id UUID NOT NULL,
    request_id UUID NOT NULL,
    step_key VARCHAR(80) NOT NULL,
    assignee_user_id UUID,
    assignee_role VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claimed_by_user_id UUID,
    join_policy VARCHAR(20),
    quorum_required INT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tasks_workflow_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances (id),
    CONSTRAINT fk_tasks_request FOREIGN KEY (request_id) REFERENCES requests (id),
    CONSTRAINT ck_tasks_status CHECK (status IN ('PENDING', 'CLAIMED', 'APPROVED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'SKIPPED')),
    CONSTRAINT ck_tasks_assignment CHECK (assignee_user_id IS NOT NULL OR assignee_role IS NOT NULL),
    CONSTRAINT ck_tasks_quorum CHECK (quorum_required IS NULL OR quorum_required >= 1)
);

CREATE INDEX idx_tasks_assignee_status_due ON tasks (assignee_user_id, status, due_at);
CREATE INDEX idx_tasks_role_status_due ON tasks (assignee_role, status, due_at);
CREATE INDEX idx_tasks_request ON tasks (request_id);
CREATE INDEX idx_tasks_instance_status ON tasks (workflow_instance_id, status);

-- H2 does not support PostgreSQL-style partial indexes.
-- Active-task uniqueness is enforced by service logic in tests/local.

CREATE TABLE task_decisions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment CLOB,
    acted_by_user_id UUID NOT NULL,
    acted_on_behalf_of_user_id UUID,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_task_decisions_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT uq_task_decisions_task_idempotency UNIQUE (task_id, idempotency_key),
    CONSTRAINT ck_task_decisions_action CHECK (action IN ('APPROVE', 'REJECT', 'SEND_BACK', 'DELEGATE'))
);
