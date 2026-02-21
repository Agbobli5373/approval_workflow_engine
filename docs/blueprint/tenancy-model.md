# Tenancy Model (v1)

## Decision

Adopt **single-tenant** operation for v1.

## Rationale

- Reduces initial complexity while building core workflow reliability.
- Avoids early ABAC expansion and tenant isolation bugs.
- Speeds MVP implementation and hardening.

## Guardrails For v2 Migration

- Keep service methods tenant-context aware in signatures where practical.
- Reserve optional `tenantId` in auth principal model.
- Avoid global uniqueness assumptions that block future `(tenant_id, key)` constraints.
- Keep data access behind repositories/services; avoid ad-hoc SQL spread.

## Explicit v1 Constraints

- One tenant data domain.
- No tenant filtering in query APIs.
- No tenant-scoped rate limiting in v1 (user/client only).

## Future v2 Path

- Add `tenant_id` columns to aggregate tables.
- Backfill existing rows with default tenant.
- Move unique indexes to tenant-scoped unique indexes.
- Add JWT `tenantId` enforcement at auth filter + repository layer.
