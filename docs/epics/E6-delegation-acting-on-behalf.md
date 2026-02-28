# E6: Delegation and Acting-on-Behalf

## Epic Goal

Allow controlled approval delegation so a delegate can act for a delegator within a strict scope and validity window, while preserving accountability in task decisions.

## Implemented Scope

- Delegation schema via `V8__delegations.sql` (PostgreSQL + H2):
  - `delegations`
- Users module delegation delivery:
  - create/list/revoke delegation APIs
  - overlap, scope, and window validation
  - runtime delegation lookup port
- Runtime authorization integration:
  - task claim/decision first checks ABAC policy
  - if denied, checks active matching delegation policy
  - delegated decisions persist `actedOnBehalfOfUserId`
- Delegation conflict handling:
  - overlapping active same-scope delegations rejected
  - multiple runtime matches rejected to avoid ambiguous actor mapping

## Implemented Artifacts

- Delegation module API and service:
  - `src/main/java/com/isaac/approvalworkflowengine/users/api/DelegationController.java`
  - `src/main/java/com/isaac/approvalworkflowengine/users/service/DelegationService.java`
  - `src/main/java/com/isaac/approvalworkflowengine/users/DelegationPolicyLookup.java`
- Delegation persistence:
  - `src/main/java/com/isaac/approvalworkflowengine/users/repository/entity/DelegationEntity.java`
  - `src/main/java/com/isaac/approvalworkflowengine/users/repository/DelegationJpaRepository.java`
- Runtime integration:
  - `src/main/java/com/isaac/approvalworkflowengine/workflowruntime/service/WorkflowRuntimeService.java`
- Migrations:
  - `src/main/resources/db/migration/postgresql/V8__delegations.sql`
  - `src/main/resources/db/migration/h2/V8__delegations.sql`

## API Endpoints

- `POST /api/delegations`
- `GET /api/delegations`
- `POST /api/delegations/{delegationId}/revoke`

## Test Coverage

- Delegation API tests:
  - `src/test/java/com/isaac/approvalworkflowengine/users/DelegationApiTest.java`
- Runtime delegation behavior tests:
  - `src/test/java/com/isaac/approvalworkflowengine/workflowruntime/TaskDelegationApiTest.java`
- Migration coverage update:
  - `src/test/java/com/isaac/approvalworkflowengine/platform/MigrationSmokeTest.java`

## Acceptance Criteria Mapping

- Delegated decisions only inside valid scope/time:
  - covered by runtime tests for valid, expired, and out-of-scope scenarios.
- Acting and represented users are both recorded:
  - covered by `TaskDelegationApiTest` asserting `actedOnBehalfOfUserId`.
- Revoked delegation is effective:
  - supported by `active=false` + `revokedAt` checks in resolver and revoke API behavior.
- Unauthorized delegation attempts fail:
  - covered by API and runtime `403` scenarios.

## Deferred

- Delegation-specific audit event persistence remains deferred to E9.
- Delegation-related outbox/integration events remain deferred to E10.
