# ADR-001: Workflow Definition Format

- Status: Accepted
- Date: 2026-02-21
- Decision Owner: Product/Architecture

## Context

The engine needs versioned, auditable workflows with sequential and parallel routing, conditional gateways, and deterministic execution.

## Decision

Adopt a **graph-based JSON model** as the source of truth for workflow versions.

Each workflow version stores:

- `definitionKey`
- `version`
- `status` (`DRAFT`, `ACTIVE`, `RETIRED`)
- `graph`:
  - `nodes[]`
  - `edges[]`
  - `policies` (join/quorum/comment requirements/escalation defaults)

## Canonical Shape (v1)

```json
{
  "nodes": [
    { "id": "start", "type": "START" },
    {
      "id": "manager_approval",
      "type": "APPROVAL",
      "assignment": { "strategy": "ROLE", "role": "MANAGER" },
      "sla": { "dueInHours": 24 }
    },
    {
      "id": "finance_gate",
      "type": "GATEWAY",
      "ruleRef": { "ruleSetKey": "expense-routing", "version": 3 }
    },
    {
      "id": "finance_approval",
      "type": "APPROVAL",
      "assignment": { "strategy": "ROLE", "role": "FINANCE" }
    },
    { "id": "end", "type": "END" }
  ],
  "edges": [
    { "from": "start", "to": "manager_approval" },
    { "from": "manager_approval", "to": "finance_gate" },
    {
      "from": "finance_gate",
      "to": "finance_approval",
      "condition": { "operator": ">", "field": "amount", "value": 1000 }
    },
    {
      "from": "finance_gate",
      "to": "end",
      "condition": { "operator": "<=", "field": "amount", "value": 1000 }
    },
    { "from": "finance_approval", "to": "end" }
  ]
}
```

## Validation Rules

- Exactly one `START` and one `END` node.
- All non-terminal nodes must have at least one outgoing edge.
- No dangling edges (`from` and `to` must reference existing nodes).
- Cycles rejected by default; allow only when `allowLoop=true` on definition policy.
- Join nodes require explicit join policy: `ALL`, `ANY`, or `QUORUM` with quorum value.
- Gateway nodes must reference a valid ruleset version.

## Activation Contract

At activation time:

1. Validate graph structure and policies.
2. Canonicalize JSON (stable key ordering + normalized arrays).
3. Compute `versionChecksum` (SHA-256 over canonical JSON).
4. Persist immutable active version.

## Consequences

- Pros: supports complex routes and future extensibility without format migration.
- Pros: enables deterministic runtime and audit replay.
- Tradeoff: validation logic is more complex than step-list format.

## Out of Scope (v1)

- Visual editor metadata.
- BPMN import/export.
- Dynamic node type plugins.
