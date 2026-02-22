# Approval Workflow Engine

Approval Workflow Engine is a Spring Boot 4 modular monolith for managing approval requests, workflow templates, and role-aware access control.

## Current Capabilities

- JWT-based local authentication (`/api/auth/login`, `/api/auth/logout`, `/api/auth/me`) in `LOCAL_AUTH` mode.
- Request lifecycle APIs: create, update, submit, cancel, get, and list.
- Workflow template APIs for admins: create definition, create version, activate version, get version.
- Flyway migrations for both H2 and PostgreSQL.
- OpenAPI/Swagger docs and actuator health endpoints.
- Correlation ID propagation and a consistent error envelope.

## Tech Stack

- Java 25 (Gradle toolchain)
- Spring Boot 4.0.3
- Spring Security, Spring Data JPA, Flyway
- Spring Modulith
- H2 (default) and PostgreSQL (local/prod)

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
- Optional: PostgreSQL if using the `local` profile
- Optional: `jq` for shell examples

## Run Locally (Default: H2 + Seed Data)

```bash
./gradlew bootRun
```

Useful URLs:

- App root: `http://localhost:8080/` (redirects to Swagger UI)
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Liveness: `http://localhost:8080/actuator/health/liveness`

### Seeded Users (default/test)

- `admin` (`WORKFLOW_ADMIN`)
- `requestor` (`REQUESTOR`)
- `approver` (`APPROVER`)
- Default password: `password`

## Quick API Smoke Test

`API-Version` is optional (defaults to `1.0`), but included here explicitly.

```bash
BASE_URL=http://localhost:8080

REQUESTOR_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "API-Version: 1.0" \
  -d '{"usernameOrEmail":"requestor","password":"password"}' | jq -r '.accessToken')

curl -s -X GET "$BASE_URL/api/requests?page=0&size=20" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0"
```

## Profiles

- Default (no profile): H2 in-memory DB, `LOCAL_AUTH`, seeded local/test data.
- `local`: PostgreSQL, `LOCAL_AUTH`, seeded local/test data.
- `prod`: PostgreSQL, `OIDC_RESOURCE_SERVER`, no local auth endpoints.

Run with local PostgreSQL profile:

```bash
export APP_DB_URL=jdbc:postgresql://localhost:5432/approval_workflow_engine
export APP_DB_USERNAME=approval
export APP_DB_PASSWORD=approval
./gradlew bootRun --args='--spring.profiles.active=local'
```

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

- Architecture and locked decisions: `docs/blueprint/README.md`
- Delivery epics: `docs/epics/README.md`
- End-to-end API test flows and payloads: `docs/test-data/controller-test-data-guide.md`
