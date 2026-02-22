# E5: Runtime Workflow and Task Engine

## Epic Goal

Execute submitted requests through workflow runtime instances, generate actionable tasks, and safely process task claim/decision flows under concurrency.

## Implemented Scope

- Runtime schema via `V7__workflow_runtime_instances_tasks.sql` (PostgreSQL + H2):
  - `workflow_instances`
  - `tasks`
  - `task_decisions`
- Runtime module delivery in `workflowruntime`:
  - JPA entities/repositories/services/controllers
  - runtime graph traversal for `START`, `APPROVAL`, `GATEWAY`, `JOIN`, `END`
- Request integration:
  - submit now bootstraps runtime synchronously
  - submit transitions to `IN_REVIEW` unless workflow finishes immediately
  - cancel on `IN_REVIEW` cancels active runtime tasks/instance
- Task APIs:
  - `GET /api/tasks`
  - `POST /api/tasks/{taskId}/claim`
  - `POST /api/tasks/{taskId}/decisions`
- Decision behavior:
  - `APPROVE`, `REJECT`, `SEND_BACK` supported
  - `DELEGATE` returns `409 CONFLICT` in E5
  - comments required for `REJECT` and `SEND_BACK`
- Join behavior:
  - `ALL`, `ANY`, `QUORUM` supported
  - for `ANY`/`QUORUM`, remaining pending sibling tasks are auto-marked `SKIPPED`
- Runtime policy defaults:
  - claim required before decision actions
  - RULE assignment strategy rejected at runtime in E5
  - task list default (`assignedTo` omitted) = me + role union
- Gateway activation validation strengthened:
  - exactly two outgoing edges
  - explicit `condition.branch=true|false`
- Local/test seed alignment:
  - default EXPENSE workflow now uses `APPROVER` role

## Implemented Artifacts

- Runtime service: `src/main/java/com/isaac/approvalworkflowengine/workflowruntime/service/WorkflowRuntimeService.java`
- Task controller: `src/main/java/com/isaac/approvalworkflowengine/workflowruntime/api/TaskController.java`
- Request integration: `src/main/java/com/isaac/approvalworkflowengine/requests/service/RequestLifecycleService.java`
- Runtime migrations:
  - `src/main/resources/db/migration/postgresql/V7__workflow_runtime_instances_tasks.sql`
  - `src/main/resources/db/migration/h2/V7__workflow_runtime_instances_tasks.sql`
- Gateway branch validation:
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/validation/WorkflowGraphValidator.java`

## Test Coverage

- `src/test/java/com/isaac/approvalworkflowengine/workflowruntime/WorkflowRuntimeSubmitIntegrationTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/workflowruntime/TaskClaimApiTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/workflowruntime/TaskDecisionApiTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/workflowruntime/JoinPolicyIntegrationTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/workflowtemplate/WorkflowGatewayBranchValidationTest.java`
- Updated request and migration coverage:
  - `src/test/java/com/isaac/approvalworkflowengine/requests/RequestLifecycleApiTest.java`
  - `src/test/java/com/isaac/approvalworkflowengine/platform/MigrationSmokeTest.java`

## Deferred to Later Epics

- Delegation execution behavior (E6)
- SLA/escalation scheduler behavior (E7/E8)
- Full append-only audit writes (E9)
- Transactional outbox publishing (E10)
