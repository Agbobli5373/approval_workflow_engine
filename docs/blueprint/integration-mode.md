# Integration Mode (v1)

## Decision

Implement **webhooks-first** integration delivery, backed by **Transactional Outbox**.

Broker support (Kafka/RabbitMQ) is deferred to v2 behind a publisher interface.

## Event Set (v1)

- `REQUEST_SUBMITTED`
- `TASK_ASSIGNED`
- `TASK_DECIDED`
- `REQUEST_APPROVED`
- `REQUEST_REJECTED`

## Delivery Contract

- Persist domain event in outbox in same DB transaction as state change.
- Publisher worker delivers to webhook subscribers asynchronously.
- Retries with exponential backoff.
- Dead-letter status and replay tooling for failed deliveries.

## Webhook Security

- HMAC signature header with shared secret.
- Include timestamp and nonce headers.
- Reject stale timestamps (default 5-minute skew window).
- Preserve event id for consumer-side idempotency.

## v2 Broker Extension

Introduce `IntegrationPublisher` interface implementations:

- `WebhookPublisher` (v1)
- `KafkaPublisher` (v2 optional)
- `RabbitPublisher` (v2 optional)

Outbox payload schema remains canonical, transport-specific mapping happens in publisher adapters.
