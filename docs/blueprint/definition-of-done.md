# Definition of Done (Per Epic)

## Global Done Criteria (Applies To Every Epic)

- Flyway migrations included and reversible strategy documented.
- API changes reflected in `openapi.yaml`.
- Unit + integration tests added for success and failure paths.
- Security checks enforced (RBAC + ABAC where applicable).
- Audit event coverage added for critical state transitions.
- Outbox event coverage added for integration-relevant transitions.
- Metrics/logging/tracing instrumentation added for new flows.
- Documentation updated in `/docs/epics` and `/docs/blueprint`.

## Epic-Specific Minimum Gates

### E0 Platform Foundation

- Project boots with modular package structure.
- Global error envelope implemented and tested.
- Correlation id propagation available in logs.

### E1 Security

- Unauthorized returns 401, forbidden returns 403.
- Role and policy negative tests pass.

### E2 Requests

- Transition matrix enforced.
- Idempotent submit covered by tests.

### E3 Templates

- Activation validation rejects invalid graphs.
- Active versions immutable.

### E4 Rules

- All operators tested.
- Deterministic evaluation test proves stable output.

### E5 Runtime/Tasks

- Claim/decision APIs concurrency-tested.
- No duplicate decisions or double-advance.

### E6 Delegation

- Scope and window constraints enforced.
- "Acted on behalf of" persisted and audited.

### E7 SLA/Escalation

- Scheduler idempotency proven with rerun tests.
- Escalation stage dedupe guaranteed.

### E8 Notifications

- Async dispatch with retries and dead-letter behavior.
- Delivery metrics exported.

### E9 Audit

- 100% critical transitions audited.
- Hash chain verification utility passes.

### E10 Integrations/Outbox

- Outbox write is in same transaction as domain update.
- Webhook signature verification tests pass.

### E11 Observability/Quality

- Required dashboards/metrics available.
- Contract and concurrency suites green in CI.
- Performance test baseline captured and documented.

## Release Gate (Before Production)

- All epic gates passed.
- Zero critical/high unresolved defects.
- Runbook and rollback plan reviewed.
- Load test and failure-mode test reports approved.
