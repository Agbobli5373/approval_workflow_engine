# E7: SLA Scheduler and Escalations

## Epic Goal

Enforce task due dates and escalation policies through reliable, idempotent background scheduling.

## Features To Implement

- Task SLA fields (`dueAt`, policy references).
- Escalation policy model:
  - reminder stages
  - reassignment stage
  - manager/backup escalation
- Scheduler worker scanning overdue tasks in paged batches.
- Idempotent escalation execution with dedupe key (`task + policy + stage`).
- Escalation event and audit emission.

## Detailed Implementation Guide

1. Extend task schema with SLA metadata and next escalation checkpoint.
2. Define escalation policy structure and versioning rules.
3. Implement scheduler job:
   - fetch overdue tasks in pages
   - lock/process in bounded batches
   - persist stage progression
4. Implement dedupe storage/constraint to prevent repeated escalation stage processing.
5. Implement escalation actions (notify, reassign, escalate-to-role/manager).
6. Add backoff and jitter to avoid job storms under backlog.
7. Add operational controls:
   - max records per run
   - lag metrics
   - failure counters
8. Add tests:
   - repeated scheduler run idempotency
   - stage progression correctness
   - heavy backlog behavior

## Deliverables

- SLA policy and scheduler worker.
- Escalation processing service with idempotency guards.
- Monitoring signals for scheduler health.

## Acceptance Criteria

- Overdue tasks are escalated according to policy stages.
- Re-running scheduler does not duplicate escalation stage side effects.
- Scheduler handles large overdue sets via paging without full table scans.
- Escalation actions are auditable and observable.
- Scheduler health endpoint/metric exposes lag and failure state.
