# E3: Workflow Template and Versioning

## Epic Goal

Enable admins to define, version, validate, activate, and retire workflow templates with integrity guarantees.

## Features To Implement

- Workflow definition aggregate (`key`, metadata, ownership).
- Workflow version lifecycle:
  - `DRAFT`
  - `ACTIVE`
  - `RETIRED`
- Template APIs:
  - `POST /workflow-definitions`
  - `POST /workflow-definitions/{key}/versions`
  - `POST /workflow-versions/{id}/activate`
  - `GET /workflow-versions/{id}`
- Graph model for steps, edges, assignments, gateways, joins.
- Activation-time graph validation.
- Version checksum generation and persistence.
- Immutability guard for activated versions.

## Detailed Implementation Guide

1. Define workflow schema tables for definitions, versions, nodes, edges, assignment policies.
2. Implement draft version creation and editing services.
3. Implement graph validator checks:
   - no orphan nodes
   - no broken edges
   - no accidental cycles
   - valid join semantics for parallel branches
4. Compute canonical JSON representation and checksum at activation.
5. Lock activated versions as immutable (application and DB-level guard).
6. Enforce one active version per definition key at a time.
7. Emit audit events for create/update/activate/retire actions.
8. Add integration tests for activation edge cases and graph failures.

## Deliverables

- Workflow definition/version data model.
- Validation and activation service.
- Admin API endpoints for template lifecycle.

## Acceptance Criteria

- Invalid workflow graphs cannot be activated.
- Active version is immutable and checksum is persisted.
- Exactly one active version exists per workflow key.
- Retired versions remain queryable for audit/history.
- Activation emits audit events with actor and version metadata.
