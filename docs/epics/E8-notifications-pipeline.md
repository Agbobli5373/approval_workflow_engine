# E8: Notifications Pipeline

## Epic Goal

Deliver asynchronous, retryable notifications for task and request lifecycle events without blocking core decision flows.

## Features To Implement

- Notification event types:
  - task assigned
  - task nearing SLA breach
  - request approved
  - request rejected
- Async notification dispatcher worker.
- Retry/backoff strategy and dead-letter visibility.
- Optional user/channel preference model.
- Notification delivery audit trail.

## Detailed Implementation Guide

1. Define notification message schema with event type, recipient, channel, payload.
2. Create notification queue source (internal table/stream or outbox-driven handoff).
3. Implement dispatcher with channel adapters (email initially; SMS/push extensible).
4. Add retry policy with max attempts and exponential backoff.
5. Add dead-letter store for exhausted failures and replay capability.
6. Add preference resolution layer (on/off by event type and channel).
7. Add metrics (sent, failed, retry count, DLQ size).
8. Add tests:
   - successful delivery path
   - transient failure retries
   - permanent failure DLQ path

## Deliverables

- Notification domain and dispatcher worker.
- Channel adapter abstraction.
- Metrics and failure visibility.

## Acceptance Criteria

- Notification sending is asynchronous and does not block request/task transactions.
- Transient failures are retried automatically with bounded attempts.
- Exhausted failures are moved to dead-letter with replay metadata.
- Notification preferences are honored when enabled.
- Dispatch metrics available for operational dashboards.
