# Database Schema Plan (PostgreSQL + Flyway)

## Scope

This plan defines baseline schema objects and migration order for the Approval Workflow Engine.

## Naming Conventions

- Tables: `snake_case` singular/plural by domain consistency.
- Primary keys: `uuid` (`id`).
- Timestamps: `created_at`, `updated_at` (`timestamptz`).
- Soft delete: avoid in audit and outbox tables.

## Migration Order

1. `V1__platform_foundation.sql` (E0: `idempotency_keys`, `outbox_events`, `job_locks`)
2. `V2__users_and_roles.sql` (E1: `users`, `user_roles`, `auth_token_revocations`)
3. `V3__requests_lifecycle.sql` (E2: `requests`, `request_status_transitions`)
4. `V4__workflow_definitions_and_versions.sql`
5. `V5__workflow_graph_nodes_edges.sql`
6. `V6__rulesets.sql`
7. `V7__workflow_runtime_instances_tasks.sql`
8. `V8__delegations.sql`
9. `V9__audit_events.sql`
10. `V10__outbox_and_webhooks.sql`

## E0 Implementation Note

- E0 applies the foundational migration as `V1__platform_foundation.sql`.
- Domain schema migrations start at `V2` in subsequent epics.

## E1 Implementation Note

- E1 applies `V2__users_and_roles.sql` in both PostgreSQL and H2 migration tracks.
- Local/test profiles also run repeatable seed scripts under:
  - `db/seed/localtest/postgresql/`
  - `db/seed/localtest/h2/`

## E2 Implementation Note

- E2 applies `V3__requests_lifecycle.sql` in both PostgreSQL and H2 migration tracks.
- Lifecycle transition history is persisted in `request_status_transitions`.

## Core Tables

### `users`

- `id uuid pk`
- `external_subject varchar(128) unique not null` (JWT `sub`)
- `email varchar(320) unique not null`
- `display_name varchar(160) not null`
- `department varchar(80)`
- `employee_id varchar(80) null`
- `password_hash varchar(120) null` (used in `LOCAL_AUTH`)
- `active boolean not null default true`
- timestamps

Indexes:

- `idx_users_department`

### `user_roles`

- `id uuid pk`
- `user_id uuid not null`
- `role_code varchar(64) not null`
- unique `(user_id, role_code)`

### `auth_token_revocations`

- `jti varchar(64) pk`
- `revoked_at timestamptz not null`
- `expires_at timestamptz not null`

Indexes:

- `idx_auth_token_revocations_expires_at`

## Workflow Definition Tables

### `workflow_definitions`

- `id uuid pk`
- `definition_key varchar(100) not null unique`
- `name varchar(150) not null`
- `request_type varchar(80) not null`
- `owner_user_id uuid not null`
- timestamps

### `workflow_versions`

- `id uuid pk`
- `workflow_definition_id uuid not null`
- `version_no int not null`
- `status varchar(20) not null` (`DRAFT|ACTIVE|RETIRED`)
- `graph_json jsonb not null`
- `checksum_sha256 char(64) null`
- `activated_at timestamptz null`
- `activated_by_user_id uuid null`
- unique `(workflow_definition_id, version_no)`

Indexes:

- `idx_workflow_versions_definition_status`

Constraint strategy:

- enforce max one active version per definition with partial unique index:
  - unique `(workflow_definition_id)` where `status='ACTIVE'`

### `workflow_nodes`

- `id uuid pk`
- `workflow_version_id uuid not null`
- `node_key varchar(80) not null`
- `node_type varchar(40) not null`
- `config_json jsonb not null`
- unique `(workflow_version_id, node_key)`

### `workflow_edges`

- `id uuid pk`
- `workflow_version_id uuid not null`
- `from_node_key varchar(80) not null`
- `to_node_key varchar(80) not null`
- `condition_json jsonb null`

Indexes:

- `idx_workflow_edges_version_from`
- `idx_workflow_edges_version_to`

## Rule Tables

### `rule_sets`

- `id uuid pk`
- `rule_set_key varchar(100) not null`
- `version_no int not null`
- `dsl_json jsonb not null`
- `checksum_sha256 char(64) not null`
- unique `(rule_set_key, version_no)`

## Request Tables

### `requests`

- `id uuid pk`
- `request_type varchar(80) not null`
- `title varchar(200) not null`
- `description text null`
- `payload_json jsonb not null`
- `amount numeric(18,2) null`
- `currency char(3) null`
- `department varchar(80) null`
- `cost_center varchar(80) null`
- `attachments_json jsonb null`
- `status varchar(30) not null`
- `requestor_user_id uuid not null`
- `workflow_version_id uuid null`
- `version bigint not null` (optimistic lock)
- timestamps

Indexes:

- `idx_requests_requestor_status`
- `idx_requests_status_created`
- `idx_requests_type_created`
- GIN index on `payload`

### `request_status_transitions` (optional but recommended)

- `id uuid pk`
- `request_id uuid not null`
- `from_status varchar(30) not null`
- `to_status varchar(30) not null`
- `changed_by_subject varchar(128) not null`
- `reason text null`
- `changed_at timestamptz not null`

## Runtime Tables

### `workflow_instances`

- `id uuid pk`
- `request_id uuid not null unique`
- `workflow_version_id uuid not null`
- `status varchar(30) not null`
- `current_step_keys jsonb null`
- `version bigint not null`
- timestamps

### `tasks`

- `id uuid pk`
- `workflow_instance_id uuid not null`
- `request_id uuid not null`
- `step_key varchar(80) not null`
- `assignee_user_id uuid null`
- `assignee_role varchar(64) null`
- `status varchar(20) not null`
- `due_at timestamptz null`
- `claimed_at timestamptz null`
- `claimed_by_user_id uuid null`
- `join_policy varchar(20) null`
- `quorum_required int null`
- `version bigint not null`
- timestamps

Indexes:

- `idx_tasks_assignee_status_due`
- `idx_tasks_role_status_due`
- `idx_tasks_request`
- `idx_tasks_instance_status`

### `task_decisions`

- `id uuid pk`
- `task_id uuid not null`
- `action varchar(20) not null`
- `comment text null`
- `acted_by_user_id uuid not null`
- `acted_on_behalf_of_user_id uuid null`
- `idempotency_key varchar(120) not null`
- `created_at timestamptz not null`
- unique `(task_id, idempotency_key)`

## Delegation Tables

### `delegations`

- `id uuid pk`
- `delegator_user_id uuid not null`
- `delegate_user_id uuid not null`
- `scope_json jsonb not null`
- `valid_from timestamptz not null`
- `valid_until timestamptz not null`
- `active boolean not null default true`
- timestamps

Indexes:

- `idx_delegations_delegator_window`
- `idx_delegations_delegate_window`

## SLA/Escalation Tables

### `escalation_policies`

- `id uuid pk`
- `policy_key varchar(100) not null unique`
- `version_no int not null`
- `policy_json jsonb not null`
- `active boolean not null default true`

### `task_escalation_runs`

- `id uuid pk`
- `task_id uuid not null`
- `policy_id uuid not null`
- `stage_no int not null`
- `dedupe_key varchar(180) not null`
- `executed_at timestamptz not null`
- unique `(dedupe_key)`

## Audit Tables

### `audit_events`

- `id uuid pk`
- `request_id uuid not null`
- `workflow_version_id uuid null`
- `event_type varchar(60) not null`
- `actor_user_id uuid null`
- `occurred_at timestamptz not null`
- `metadata_json jsonb not null`
- `prev_hash char(64) null`
- `hash char(64) not null`

Indexes:

- `idx_audit_request_time`
- `idx_audit_event_type_time`

## Outbox and Webhook Tables

### `outbox_events`

- `id uuid pk`
- `event_type varchar(60) not null`
- `aggregate_type varchar(60) not null`
- `aggregate_id uuid not null`
- `payload_json jsonb not null`
- `status varchar(20) not null` (`PENDING|PROCESSING|PUBLISHED|FAILED|DEAD`)
- `attempt_count int not null default 0`
- `next_attempt_at timestamptz null`
- `last_error text null`
- `created_at timestamptz not null`
- `published_at timestamptz null`

Indexes:

- `idx_outbox_status_next_attempt`
- `idx_outbox_aggregate`

### `webhook_subscriptions`

- `id uuid pk`
- `target_url text not null`
- `secret_ciphertext text not null`
- `event_filters jsonb not null`
- `active boolean not null default true`
- `created_by_user_id uuid not null`
- timestamps

### `webhook_deliveries`

- `id uuid pk`
- `outbox_event_id uuid not null`
- `webhook_subscription_id uuid not null`
- `delivery_status varchar(20) not null`
- `http_status int null`
- `attempt_count int not null default 0`
- `last_error text null`
- `delivered_at timestamptz null`
- timestamps

Indexes:

- `idx_webhook_deliveries_status`
- `idx_webhook_deliveries_outbox`

## Idempotency and Job Lock Tables

### `idempotency_keys`

- `id uuid pk`
- `scope varchar(80) not null` (`SUBMIT`, `DECIDE`, `ESCALATE`)
- `key_value varchar(120) not null`
- `request_hash char(64) not null`
- `response_json jsonb null`
- `created_at timestamptz not null`
- unique `(scope, key_value)`

### `job_locks`

- `lock_name varchar(120) pk`
- `locked_until timestamptz not null`
- `locked_by varchar(120) not null`
- `updated_at timestamptz not null`

## Foreign Key Policy

- Use FKs for core integrity on all primary relationships.
- Avoid cascading deletes for audit/outbox/history tables.
- Prefer logical cancellation over hard delete for business aggregates.
