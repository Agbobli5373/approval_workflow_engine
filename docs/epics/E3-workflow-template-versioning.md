# E3: Workflow Template and Versioning

## Epic Goal

Enable admins to define, version, validate, activate, and retire workflow templates with integrity guarantees.

## Implemented Features

- Workflow definition aggregate (`key`, metadata, ownership).
- Workflow version lifecycle:
  - `DRAFT`
  - `ACTIVE`
  - `RETIRED`
- Admin template APIs under `/api` (API version header applies):
  - `POST /api/workflow-definitions`
  - `POST /api/workflow-definitions/{definitionKey}/versions`
  - `POST /api/workflow-versions/{workflowVersionId}/activate`
  - `GET /api/workflow-versions/{workflowVersionId}`
- Graph persistence in two forms:
  - canonical `workflow_versions.graph_json`
  - normalized `workflow_nodes` + `workflow_edges`
- Activation validation rules:
  - exactly one `START` and one `END`
  - no dangling edges
  - all nodes reachable from `START`
  - all nodes can reach `END`
  - cycles rejected unless `allow_loopback=true`
  - `JOIN` requires valid policy/quorum
  - `GATEWAY` validates `ruleRef` shape (syntax-only in E3)
  - `APPROVAL` validates assignment strategy shape
- SHA-256 checksum generation over canonicalized graph JSON on activation.
- Activated versions immutable (no edit endpoint, mutable state only in `DRAFT`).
- Auto-retirement of prior `ACTIVE` version when new version is activated.
- Request submission now binds to DB-backed active workflow versions by request type.

## Implementation Artifacts

- Migrations:
  - `src/main/resources/db/migration/postgresql/V4__workflow_definitions_and_versions.sql`
  - `src/main/resources/db/migration/postgresql/V5__workflow_graph_nodes_edges.sql`
  - `src/main/resources/db/migration/h2/V4__workflow_definitions_and_versions.sql`
  - `src/main/resources/db/migration/h2/V5__workflow_graph_nodes_edges.sql`
- Local/test seeds:
  - `src/main/resources/db/seed/localtest/postgresql/R__localtest_seed_workflow_templates.sql`
  - `src/main/resources/db/seed/localtest/h2/R__localtest_seed_workflow_templates.sql`
- Workflow template module:
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/api/WorkflowTemplateController.java`
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/service/WorkflowTemplateService.java`
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/validation/WorkflowGraphValidator.java`
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/checksum/WorkflowGraphChecksumService.java`
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/repository/entity/WorkflowDefinitionEntity.java`
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/repository/entity/WorkflowVersionEntity.java`
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/repository/entity/WorkflowNodeEntity.java`
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/repository/entity/WorkflowEdgeEntity.java`
- Request resolver integration:
  - `src/main/java/com/isaac/approvalworkflowengine/requests/service/DatabaseBackedRequestWorkflowVersionResolver.java`
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/WorkflowTemplateLookup.java`

## Tests Added/Updated

- `src/test/java/com/isaac/approvalworkflowengine/workflowtemplate/WorkflowTemplateApiTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/requests/RequestLifecycleApiTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/platform/MigrationSmokeTest.java`

## Acceptance Criteria

- Invalid workflow graphs cannot be activated.
- Active version is immutable and checksum is persisted.
- Exactly one active version exists per workflow key.
- Retired versions remain queryable for audit/history.
- Request submit resolves active workflow versions from DB (no property fallback).

## Deferred To Later Epics

- Full audit ledger writes and hash-chain coverage (E9).
- Outbox/integration events for template lifecycle (E10).
- Ruleset existence/version checks against rule tables (E4).
