# E11: Observability, Reliability, and Quality Gates

## Epic Goal

Harden the system for production operations with deep observability, performance safeguards, and release quality gates.

## Features To Implement

- Structured JSON logs with correlation ids.
- Metrics:
  - task decisions count
  - decision latency
  - SLA breach count
  - outbox backlog size
- OpenTelemetry tracing across API and background workers.
- Health endpoints:
  - liveness/readiness
  - DB connectivity
  - scheduler health
  - outbox publisher health
- Performance/indexing plan for core query paths.
- Full automated testing matrix:
  - unit
  - integration (Testcontainers + PostgreSQL)
  - concurrency
  - contract
- Release gates and SLO checks.

## Detailed Implementation Guide

1. Add logging formatters and MDC enrichment (correlation id, actor id, request id).
2. Instrument REST endpoints and workers with trace spans.
3. Instrument decision, escalation, and outbox flows with business metrics.
4. Implement health indicators for DB, scheduler, and outbox pipelines.
5. Add DB indexing for key workloads:
   - tasks by assignee/status/due date
   - requests by status/type/date
   - audit by request id/time
6. Run load/perf tests aligned to target scale (100k requests/month, 1M audit events/month).
7. Build automated CI stages with fail-fast gates for tests and schema validation.
8. Add operational runbooks for incident triage and backlog recovery.

## Deliverables

- Complete observability instrumentation.
- Health and operational readiness checks.
- Performance benchmark report and index tuning changes.
- CI quality gates and release checklist.

## Acceptance Criteria

- Critical user actions and worker jobs are traceable by correlation id.
- Required metrics are exported and visible on dashboards.
- Health checks accurately report degraded dependencies.
- Concurrency and contract tests pass in CI.
- Performance tests demonstrate acceptable behavior at target scale.
