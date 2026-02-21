# E1: Security and Access Control

## Epic Goal

Deliver production-ready authentication/authorization with dual-mode behavior:

- `LOCAL_AUTH` for local/test with DB-seeded users and app login/logout.
- `OIDC_RESOURCE_SERVER` for production-compatible external JWT validation.

## Implemented Features

- Dual-mode security mode property: `app.security.mode`.
- JWT bearer auth for all protected routes.
- Local/test auth endpoints:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/logout`
  - `GET /api/v1/auth/me`
- RBAC protection:
  - Swagger/OpenAPI endpoints are public in `LOCAL_AUTH` and require `ROLE_WORKFLOW_ADMIN` in `OIDC_RESOURCE_SERVER`.
  - Liveness endpoint is public.
  - All other routes require authentication.
- Role extraction from JWT:
  - primary claim `roles`
  - fallback claim `realm_access.roles` (Keycloak shape)
- ABAC foundation service:
  - `AccessPolicyService`
  - `canEditRequest`
  - `canDecideTask`
  - `canActivateWorkflow`
- In-memory rate limiting:
  - authenticated subject key (`sub`) limit default `120/min`
  - anonymous remote-address key limit default `30/min`
  - breach returns `429` with `Retry-After` and standard `ApiError`.
- Logout revocation with persisted `jti` deny-list (`auth_token_revocations`).

## Implementation Artifacts

- Security config:
  - `src/main/java/com/isaac/approvalworkflowengine/auth/security/SecurityConfiguration.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/security/AppSecurityProperties.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/security/JwtAuthoritiesConverter.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/security/LocalJwtConfiguration.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/security/RevokedTokenFilter.java`
- Auth API/service/repository:
  - `src/main/java/com/isaac/approvalworkflowengine/auth/api/AuthController.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/service/LocalAuthenticationService.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/repository/JdbcUserAccountRepository.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/repository/JdbcTokenRevocationRepository.java`
- ABAC foundation:
  - `src/main/java/com/isaac/approvalworkflowengine/auth/policy/AccessPolicyService.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/policy/DefaultAccessPolicyService.java`
- Rate limiting:
  - `src/main/java/com/isaac/approvalworkflowengine/auth/ratelimit/RateLimitingFilter.java`
  - `src/main/java/com/isaac/approvalworkflowengine/auth/ratelimit/InMemoryRateLimiterService.java`

## Database and Seed Data

- Added migration `V2__users_and_roles.sql` for both PostgreSQL and H2.
- Added tables:
  - `users`
  - `user_roles`
  - `auth_token_revocations`
- Added local/test repeatable seed scripts:
  - `db/seed/localtest/postgresql/R__localtest_seed_users.sql`
  - `db/seed/localtest/h2/R__localtest_seed_users.sql`
- Seed users (local/test only):
  - `admin` -> `WORKFLOW_ADMIN`
  - `requestor` -> `REQUESTOR`
  - `approver` -> `APPROVER`

## Tests Added/Updated

- `src/test/java/com/isaac/approvalworkflowengine/AuthenticationFlowTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/RateLimitingFilterTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/AccessPolicyServiceTest.java`
- `src/test/java/com/isaac/approvalworkflowengine/MigrationSmokeTest.java` (E1 schema + seed assertions)
- Existing E0 tests remain passing.

## Acceptance Criteria Status

- Non-public endpoints reject anonymous access: satisfied.
- Swagger/OpenAPI access is mode-specific (public local/test, admin-only prod): satisfied.
- Local/test login/logout/me functional with seeded users: satisfied.
- Production mode remains OIDC resource-server compatible: satisfied by mode-switch config.
- 401/403/429 use standard `ApiError` envelope with correlation id: satisfied.
- Rate limiting active and tested: satisfied.
