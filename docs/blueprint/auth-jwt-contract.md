# Auth and JWT Claims Contract (v1)

## Decision

Use a dual-mode strategy:

- `LOCAL_AUTH` (local/test): app-managed login/logout with DB users and HMAC JWT issuance.
- `OIDC_RESOURCE_SERVER` (prod): external OIDC JWT validation via issuer/JWKs.

This preserves production OIDC alignment while enabling zero-setup local/test authentication.

## Security Modes

### `LOCAL_AUTH`

- Enabled in `local` and `test` profiles.
- Login endpoint: `POST /api/v1/auth/login`.
- Logout endpoint: `POST /api/v1/auth/logout`.
- Current user endpoint: `GET /api/v1/auth/me`.
- Token revocation on logout via persisted `jti` in `auth_token_revocations`.
- JWT signature: `HS256` with `app.security.jwt.hmac-secret`.

### `OIDC_RESOURCE_SERVER`

- Enabled in `prod` profile.
- Requires `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
- Local login/logout controller is not active.
- JWT validation delegated to resource server decoder.

## JWT Claim Contract

### Required (both modes)

- `sub`: principal identifier.
- `iat`: issued-at.
- `exp`: expiry.

### Role claims (mapping order)

1. `roles` claim (array of strings).
2. fallback `realm_access.roles` (Keycloak-compatible).

Mapped authorities are normalized to `ROLE_*`.

### Local mode additional claims

- `jti`
- `uid`
- `email`
- `displayName`
- `department` (if available)
- `employeeId` (if available)

## Role Taxonomy (RBAC)

- `REQUESTOR`
- `APPROVER`
- `WORKFLOW_ADMIN`
- `AUDITOR`
- `INTEGRATOR`
- `SYSTEM_JOB`

## Endpoint Rules

- Public:
  - `POST /api/v1/auth/login` in `LOCAL_AUTH` only.
  - `/actuator/health/liveness`.
  - `/swagger-ui/**` and `/v3/api-docs/**` in `LOCAL_AUTH`.
- Admin-only:
  - `/swagger-ui/**` and `/v3/api-docs/**` in `OIDC_RESOURCE_SERVER`.
- All other endpoints require authenticated bearer token.

## Authorization Split

- RBAC at route/controller level.
- ABAC in `AccessPolicyService` for domain decisions.

Current ABAC foundation methods:

- `canEditRequest`
- `canDecideTask`
- `canActivateWorkflow`

## Error Contract

Security failures use the standard envelope:

```json
{
  "code": "FORBIDDEN",
  "message": "Access denied",
  "correlationId": "01HT...",
  "details": []
}
```

Also applies to `401` and `429` responses.
