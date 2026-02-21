# E0: Platform Foundation (Spring Boot 4 Modular Monolith)

## Epic Goal

Establish a production-ready Spring Boot 4 baseline with clear module boundaries, shared platform primitives, and deployment-safe defaults.

## Features To Implement

- Spring Boot 4 project baseline with Java 25.
- Initial module structure:
  - `auth`
  - `users`
  - `requests`
  - `workflow-template`
  - `workflow-runtime`
  - `rules`
  - `audit`
  - `notifications`
  - `integrations-outbox`
- PostgreSQL + Flyway integration.
- Standard API error contract (`code`, `message`, `correlationId`, `details`).
- Shared concerns:
  - request correlation id filter
  - exception mapping
  - validation error mapping
  - pagination utilities
- Baseline health endpoints and app configuration profiles (`local`, `test`, `prod`).
- OpenAPI documentation setup with versioning strategy.

## Detailed Implementation Guide

1. Create package layout by module and keep internals package-private.
2. Add core dependencies and enforce versions via BOMs (Spring Boot + Spring Modulith where needed).
3. Configure datasource, Flyway, and baseline migration naming conventions.
4. Create foundational tables needed globally (idempotency key registry, outbox table shell, common enum strategy if used).
5. Implement a global exception handler with deterministic error JSON.
6. Add request correlation middleware/filter and include correlation id in logs.
7. Add base OpenAPI scaffolding and API versioning strategy (`/api/v1`).
8. Add smoke tests:
   - context boot test
   - DB connectivity test
   - Flyway migration startup test

## Deliverables

- Buildable service skeleton.
- Flyway baseline migration scripts.
- Shared API error model.
- Module package structure committed.

## Acceptance Criteria

- Application boots with all modules wired and no circular package dependencies.
- `./gradlew test` passes for baseline tests.
- Flyway applies migrations successfully on empty database.
- Every error response includes `correlationId`.
- Health endpoint reports app and DB status.

## Implemented Artifacts (E0)

- Base package normalized to `com.isaac.approvalworkflowengine`.
- Modulith module roots created:
  - `auth`
  - `users`
  - `requests`
  - `workflowtemplate`
  - `workflowruntime`
  - `rules`
  - `audit`
  - `notifications`
  - `integrationsoutbox`
  - `shared`
- Foundational Flyway migrations:
  - `src/main/resources/db/migration/postgresql/V1__platform_foundation.sql`
  - `src/main/resources/db/migration/h2/V1__platform_foundation.sql`
- Shared platform primitives:
  - correlation id filter
  - global exception handler and API error envelope
  - pagination utility baseline
  - profile-based configuration (`application-local`, `application-test`, `application-prod`)

## Baseline Test Suite (E0)

- `ApprovalWorkflowEngineApplicationTests`: default profile context boot.
- `ModulithArchitectureTest`: module verification and cycle checks.
- `MigrationSmokeTest`: H2 migration execution and foundation table presence.
- `ErrorEnvelopeAndCorrelationTest`: validation/runtime/conflict envelope mapping and correlation header behavior.
- `ActuatorHealthEndpointTest`: liveness/readiness endpoint checks with DB component.
- `OpenApiContractValidationTest`: parse/validate `docs/blueprint/openapi.yaml`.
