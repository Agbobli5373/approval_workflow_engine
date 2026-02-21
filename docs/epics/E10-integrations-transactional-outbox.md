# E10: Integrations and Transactional Outbox

## Epic Goal

Publish reliable integration events and webhooks with guaranteed consistency between domain state and external notifications.

## Features To Implement

- Domain events:
  - `REQUEST_SUBMITTED`
  - `TASK_ASSIGNED`
  - `TASK_DECIDED`
  - `REQUEST_APPROVED`
  - `REQUEST_REJECTED`
- Transactional outbox table and writer.
- Outbox publisher worker with retry/backoff and failure visibility.
- Webhook subscription management APIs:
  - `POST /webhooks/subscriptions`
  - `GET /webhooks/subscriptions`
- Webhook delivery with HMAC signing and replay protection.
- Optional broker adapter abstraction (Kafka/RabbitMQ) behind publisher interface.

## Detailed Implementation Guide

1. Define canonical event envelope (`eventId`, `type`, `occurredAt`, `aggregateId`, `payload`, `version`).
2. Write outbox rows in same transaction as domain state changes.
3. Implement publisher worker polling outbox in batches with row state transitions.
4. Implement retry and poison/dead-letter handling for publish failures.
5. Implement webhook subscription model (target URL, secret, active status, event filters).
6. Sign webhook payload with HMAC, include timestamp/nonce headers.
7. Implement consumer replay protection guidance and signature verification docs.
8. Add idempotent delivery handling by event id at publisher level.
9. Add tests:
   - outbox write atomicity with domain transaction
   - retry behavior
   - webhook signature generation and verification consistency

## Deliverables

- Outbox schema, writer, and publisher worker.
- Webhook subscription APIs and delivery service.
- Integration event contract documentation.

## Acceptance Criteria

- No domain event is published without corresponding committed domain state.
- Publisher retries transient failures and surfaces stuck messages.
- Webhooks are signed and include replay-protection fields.
- Subscription APIs support create/list and status control.
- Outbox backlog metrics expose processing health.
