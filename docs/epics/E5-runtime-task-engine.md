# E5: Runtime Workflow and Task Engine

## Epic Goal

Execute submitted requests through workflow runtime instances, generate actionable tasks, and safely process decisions under concurrency.

## Features To Implement

- Workflow instance creation at request submission.
- Task generation for sequential and parallel steps.
- Task statuses:
  - `PENDING`
  - `CLAIMED`
  - `APPROVED`
  - `REJECTED`
  - `CANCELLED`
  - `EXPIRED`
  - `SKIPPED`
- Task APIs:
  - `GET /tasks?assignedTo=me&status=PENDING`
  - `POST /tasks/{id}/claim`
  - `POST /tasks/{id}/decisions`
- Decision actions:
  - `APPROVE`
  - `REJECT`
  - `SEND_BACK`
  - `DELEGATE`
- Parallel join semantics (`ALL`, `ANY`, `QUORUM`).
- Idempotency and optimistic locking for claim/decision actions.

## Detailed Implementation Guide

1. Design runtime schema:
   - workflow instances
   - runtime step state
   - tasks
   - decision records
2. Build runtime orchestrator to advance state machine after each decision.
3. Implement assignment resolver (user, role, rule-driven resolver output).
4. Implement claim flow with ownership and claim timestamp.
5. Implement decision flow with policy checks and required comments for `REJECT`/`SEND_BACK`.
6. Implement parallel step resolution and join completion tracking.
7. Add idempotency key handling for decision endpoint.
8. Apply optimistic locking to prevent double-advance under concurrent decisions.
9. Emit audit and outbox events transactionally with state changes.
10. Add concurrency tests for duplicate decision and parallel approval races.

## Deliverables

- Runtime engine service and persistence layer.
- Task APIs and filtering.
- Concurrency-safe decision processing.

## Acceptance Criteria

- Request submission creates exactly one workflow runtime instance.
- Duplicate decision retries do not double-apply workflow progression.
- Parallel joins respect `ALL`, `ANY`, and `QUORUM` behavior exactly.
- `REJECT` and `SEND_BACK` enforce comment policy.
- Task list endpoint returns only authorized and assigned tasks.
