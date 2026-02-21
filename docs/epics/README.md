# Approval Workflow Engine Epic Plan

This folder contains implementation-ready epic guides for the Spring Boot 4 Approval Workflow Engine.

## Recommended Delivery Order

1. [E0 - Platform Foundation](./E0-platform-foundation.md)
2. [E1 - Security and Access Control](./E1-security-access-control.md)
3. [E2 - Request Lifecycle Management](./E2-request-lifecycle-management.md)
4. [E3 - Workflow Template and Versioning](./E3-workflow-template-versioning.md)
5. [E4 - Rules Engine (JSON DSL)](./E4-rules-engine-json-dsl.md)
6. [E5 - Runtime Workflow and Task Engine](./E5-runtime-task-engine.md)
7. [E6 - Delegation and Acting-on-Behalf](./E6-delegation-acting-on-behalf.md)
8. [E7 - SLA Scheduler and Escalations](./E7-sla-scheduler-escalations.md)
9. [E8 - Notifications Pipeline](./E8-notifications-pipeline.md)
10. [E9 - Audit and Compliance Ledger](./E9-audit-compliance-ledger.md)
11. [E10 - Integrations and Transactional Outbox](./E10-integrations-transactional-outbox.md)
12. [E11 - Observability, Reliability, and Quality Gates](./E11-observability-reliability-quality.md)

## Notes

- Build as an API-first modular monolith.
- Keep strict Flyway migration discipline.
- Keep idempotency and optimistic locking in scope for all write-critical flows.
- Treat audit/outbox writes as first-class transactional concerns.

## Implementation Locks

Before coding each epic, load `/Users/agbobliisaac/Desktop/approval_workflow_engine/docs/blueprint/README.md` for locked architecture decisions, API contract, and schema plan.
