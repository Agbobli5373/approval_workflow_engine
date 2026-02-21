# Build Blueprint (Implementation Locks)

This folder resolves key open decisions so Codex can implement with minimal ambiguity.

## Read In This Order

1. `ADR-001-workflow-definition-format.md`
2. `auth-jwt-contract.md`
3. `tenancy-model.md`
4. `integration-mode.md`
5. `db-schema-plan.md`
6. `openapi.yaml`
7. `definition-of-done.md`

## Locked Decisions

- Workflow definition format: **graph-based JSON** (`nodes` + `edges`).
- Auth provider: **OIDC JWT via Keycloak** (or any OIDC-compatible provider with same claims contract).
- Tenancy: **single-tenant for v1**, with migration hooks for tenant-aware v2.
- Integrations: **webhooks first** with transactional outbox; broker adapter later.
