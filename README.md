# Approval Workflow Engine

Approval Workflow Engine is a Spring Boot 4 modular monolith for approval requests, workflow templates, rules-based routing, and runtime task decisions.

## Implementation Status

Implemented epics:

- E0 Platform Foundation
- E1 Security and Access Control
- E2 Request Lifecycle Management
- E3 Workflow Template Versioning
- E4 Rules Engine (JSON DSL)
- E5 Runtime Workflow and Task Engine

Deferred epics include delegation execution, SLA escalation, audit ledger, and transactional outbox publishing.

## Current Capabilities

- Dual-mode authentication:
  - `LOCAL_AUTH` mode with seeded users and app-issued JWT
  - `OIDC_RESOURCE_SERVER` mode for production JWT validation
- Requests API:
  - create, update, submit, cancel, get, list
  - submit binds active workflow version and starts runtime synchronously
  - submit transitions request to `IN_REVIEW` unless terminal in same transaction
- Workflow template API (admin):
  - create definition, create version, activate version, get version
  - gateway activation enforces explicit true/false branch edges
- Rules engine API (admin):
  - create/list/get ruleset versions
  - deterministic simulation with trace output
- Runtime task API:
  - list tasks
  - claim task (idempotent)
  - decide task (`APPROVE`, `REJECT`, `SEND_BACK`)
  - `DELEGATE` is present in contract but returns `409 CONFLICT` in E5
- Parallel join execution:
  - `ALL`, `ANY`, `QUORUM`
  - for `ANY` and `QUORUM`, remaining pending siblings are marked `SKIPPED`
- Platform baseline:
  - Flyway migrations for PostgreSQL and H2
  - correlation id propagation and standard `ApiError` envelope
  - actuator liveness/readiness health endpoints

## Tech Stack

- Java 25 (Gradle toolchain)
- Spring Boot 4.0.3
- Spring Data JPA, Spring Security, OAuth2 Resource Server
- Spring Modulith
- Flyway
- H2 (default/test), PostgreSQL (local/prod)

## Project Layout

```text
src/main/java/com/isaac/approvalworkflowengine/
  auth/
  requests/
  workflowtemplate/
  workflowruntime/
  rules/
  notifications/
  integrationsoutbox/
  audit/
  users/
  shared/
```

## Prerequisites

- JDK 25+
- Optional: PostgreSQL (for `local` or `prod` profiles)
- Optional: `jq` for shell examples

## Run Locally (Default Profile: H2 + Seed Data)

```bash
./gradlew bootRun
```

Useful URLs:

- App root: `http://localhost:8080/` (redirects to Swagger UI)
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Liveness: `http://localhost:8080/actuator/health/liveness`
- Readiness: `http://localhost:8080/actuator/health/readiness`

## Seeded Accounts (default/test/local)

- `admin` with role `WORKFLOW_ADMIN`
- `requestor` with role `REQUESTOR`
- `approver` with role `APPROVER`
- Default password: `password`

Local/test also seed a default active `EXPENSE` workflow template assigned to role `APPROVER`.

## API Versioning

- API base path: `/api`
- Version transport: `API-Version` header
- Default API version: `1.0`

## Quick Smoke Flow

```bash
BASE_URL=http://localhost:8080

REQUESTOR_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "API-Version: 1.0" \
  -d '{"usernameOrEmail":"requestor","password":"password"}' | jq -r '.accessToken')

APPROVER_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "API-Version: 1.0" \
  -d '{"usernameOrEmail":"approver","password":"password"}' | jq -r '.accessToken')

REQUEST_ID=$(curl -s -X POST "$BASE_URL/api/requests" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -H "API-Version: 1.0" \
  -d '{"requestType":"EXPENSE","title":"Laptop","payload":{"costCenter":"CC-100"},"amount":1800,"currency":"USD","department":"Finance"}' | jq -r '.id')

curl -s -X POST "$BASE_URL/api/requests/$REQUEST_ID/submit" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "Idempotency-Key: submit-smoke-12345" \
  -H "API-Version: 1.0" | jq .

curl -s "$BASE_URL/api/tasks?assignedTo=role&status=PENDING" \
  -H "Authorization: Bearer $APPROVER_TOKEN" \
  -H "API-Version: 1.0" | jq .
```

## Profiles

- Default (no profile):
  - H2 in-memory
  - `LOCAL_AUTH`
  - migration + localtest seeds
- `local`:
  - PostgreSQL
  - `LOCAL_AUTH`
  - migration + localtest seeds
- `prod`:
  - PostgreSQL
  - `OIDC_RESOURCE_SERVER`
  - migration only (no localtest seeds)

Run with local PostgreSQL:

```bash
export APP_DB_URL=jdbc:postgresql://localhost:5432/approval_workflow_engine
export APP_DB_USERNAME=approval
export APP_DB_PASSWORD=approval
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Security Notes

- In `LOCAL_AUTH`, Swagger/OpenAPI endpoints are accessible without admin auth for local development.
- In `OIDC_RESOURCE_SERVER`, Swagger/OpenAPI endpoints require `WORKFLOW_ADMIN`.

## Testing

```bash
./gradlew test
```

## Native Build (Optional)

Requires GraalVM 25+:

```bash
./gradlew nativeCompile
build/native/nativeCompile/approval_workflow_engine
```

## Documentation

- Blueprint and architecture decisions: `docs/blueprint/README.md`
- Epic breakdown and implementation guides: `docs/epics/README.md`
- Controller test data and sample payloads: `docs/test-data/controller-test-data-guide.md`
