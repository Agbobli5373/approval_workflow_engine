# Auth and JWT Claims Contract (v1)

## Decision

Use **OAuth2/OIDC JWT Resource Server** with **Keycloak-compatible claims**.

Any OIDC provider is acceptable if it produces this claims shape.

## Token Requirements

- Signature: asymmetric (`RS256` preferred).
- Required claims:
  - `sub` (user id)
  - `iss` (issuer)
  - `exp`
  - `iat`
  - `scope` or `scp`
- Required custom claims:
  - `roles`: array of application roles
  - `department`: string (for ABAC)
  - `employeeId`: string
- Optional future claim:
  - `tenantId` (reserved; ignored in single-tenant v1)

## Role Mapping (RBAC)

- `REQUESTOR`
- `APPROVER`
- `WORKFLOW_ADMIN`
- `AUDITOR`
- `INTEGRATOR`
- `SYSTEM_JOB` (internal worker/service principal)

## Authorization Split

- RBAC at endpoint/controller layer.
- ABAC at domain service layer.

ABAC checks include:

- assignment ownership
- delegation scope
- department boundaries
- workflow admin ownership rules

## API Security Rules

- All endpoints require JWT except liveness endpoint.
- Admin endpoints require `WORKFLOW_ADMIN`.
- Audit read requires `AUDITOR` or request-level privileged access policy.
- Worker internal endpoints require `SYSTEM_JOB`.

## Error Contract

Security errors must use standard API envelope:

```json
{
  "code": "FORBIDDEN",
  "message": "Access denied",
  "correlationId": "01HT...",
  "details": []
}
```

Never leak internal role or policy implementation details.
