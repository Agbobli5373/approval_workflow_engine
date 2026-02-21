# E9: Audit and Compliance Ledger

## Epic Goal

Provide immutable, complete, tamper-evident audit history for all critical request and task transitions.

## Features To Implement

- Append-only audit event store.
- Mandatory audit coverage for:
  - request create/update/submit/cancel
  - task assign/claim/decide/delegate/escalate
  - workflow version activation/retirement usage
- Event metadata:
  - actor
  - timestamp
  - request id
  - workflow version id
  - action details
- Tamper-evidence hash chain per request stream (`prev_hash`, `hash`).
- Audit API:
  - `GET /requests/{id}/audit`

## Detailed Implementation Guide

1. Create append-only audit schema and prohibit update/delete at application level.
2. Implement audit writer service called within business transactions.
3. Implement request-scoped hash-chain generation with canonical payload serialization.
4. Add verification utility to recompute and validate hash chain integrity.
5. Add indexes for request id, timestamp, actor, event type.
6. Implement read API with pagination and filtering.
7. Add RBAC rules so only authorized roles access full audit timelines.
8. Add tests:
   - completeness on each critical transition
   - hash chain integrity across event stream
   - unauthorized audit access denied

## Deliverables

- Audit store schema and writer.
- Tamper-evidence mechanism and verifier.
- Audit retrieval endpoint.

## Acceptance Criteria

- 100% of critical transitions produce audit records.
- Audit records are append-only through service APIs.
- Hash-chain verification passes for untampered streams.
- Any chain break is detectable via verification routine.
- Audit endpoint returns ordered, paginated timeline with metadata.
