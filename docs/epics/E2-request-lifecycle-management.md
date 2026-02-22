# E2: Request Lifecycle Management

## Epic Goal

Implement the request lifecycle with strict state transitions, ownership rules, and idempotent submit/cancel APIs.

## Implemented Features

- Request entity and status machine:
  - `DRAFT`
  - `SUBMITTED`
  - `IN_REVIEW`
  - `CHANGES_REQUESTED`
  - `APPROVED`
  - `REJECTED`
  - `CANCELLED`
  - `EXPIRED`
- Request APIs under `/api/requests`:
  - `POST /api/requests`
  - `PATCH /api/requests/{requestId}`
  - `POST /api/requests/{requestId}/submit` (`Idempotency-Key` required)
  - `POST /api/requests/{requestId}/cancel` (`Idempotency-Key` required)
  - `GET /api/requests/{requestId}`
  - `GET /api/requests`
- API versioning uses optional `API-Version` header (default `1.0`).
- Ownership enforcement:
  - non-admin actors can only access their own requests
  - `WORKFLOW_ADMIN` can access all requests
- Transition guardrails:
  - edit only in `DRAFT` and `CHANGES_REQUESTED`
  - submit only in `DRAFT` and `CHANGES_REQUESTED`
  - cancel only in `DRAFT`, `SUBMITTED`, `IN_REVIEW`, `CHANGES_REQUESTED`
- Workflow version binding on submit via property-backed resolver (`app.requests.active-workflow-versions`).
- Idempotent submit and cancel using foundational `idempotency_keys`.
- Request transition history persistence in `request_status_transitions`.

## Implementation Artifacts

- Migration scripts:
  - `src/main/resources/db/migration/postgresql/V3__requests_lifecycle.sql`
  - `src/main/resources/db/migration/h2/V3__requests_lifecycle.sql`
- Request API/service/repository:
  - `src/main/java/com/isaac/approvalworkflowengine/requests/api/RequestController.java`
  - `src/main/java/com/isaac/approvalworkflowengine/requests/service/RequestLifecycleService.java`
  - `src/main/java/com/isaac/approvalworkflowengine/requests/repository/entity/RequestEntity.java`
  - `src/main/java/com/isaac/approvalworkflowengine/requests/repository/entity/RequestStatusTransitionEntity.java`
  - `src/main/java/com/isaac/approvalworkflowengine/requests/repository/entity/IdempotencyKeyEntity.java`
- Workflow binding support:
  - `src/main/java/com/isaac/approvalworkflowengine/requests/service/RequestWorkflowBindingProperties.java`
  - `src/main/java/com/isaac/approvalworkflowengine/requests/service/PropertyBackedRequestWorkflowVersionResolver.java`
- Error handling additions used by E2:
  - `src/main/java/com/isaac/approvalworkflowengine/shared/api/GlobalExceptionHandler.java`

## Tests Added/Updated

- `src/test/java/com/isaac/approvalworkflowengine/RequestLifecycleApiTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/MigrationSmokeTest.java` (now asserts `V3` presence and request tables)
- Existing E0/E1 tests remain passing.

## Acceptance Criteria

- Request cannot be edited outside `DRAFT` and `CHANGES_REQUESTED`.
- Submit operation binds to a specific active workflow version.
- Duplicate submit and cancel calls with the same idempotency key are safe and deterministic.
- List endpoint supports pagination, filtering, and controlled sort fields.
- API returns consistent error payload for invalid transitions, ownership violations, and missing resources.
