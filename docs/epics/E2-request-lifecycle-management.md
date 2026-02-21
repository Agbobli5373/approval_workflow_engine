# E2: Request Lifecycle Management

## Epic Goal

Implement the complete request lifecycle with strict state transitions, payload validation, and API support for create/edit/submit/cancel/resubmit.

## Features To Implement

- Request entity and status machine:
  - `DRAFT`
  - `SUBMITTED`
  - `IN_REVIEW`
  - `CHANGES_REQUESTED`
  - `APPROVED`
  - `REJECTED`
  - `CANCELLED`
  - `EXPIRED`
- Request APIs:
  - `POST /requests`
  - `PATCH /requests/{id}`
  - `POST /requests/{id}/submit`
  - `POST /requests/{id}/cancel`
  - `GET /requests/{id}`
  - `GET /requests`
- JSONB payload support and validation pipeline.
- Request editing guardrails (only `DRAFT` and `CHANGES_REQUESTED`).
- Workflow version binding at submission time.
- Idempotent submit operation.

## Detailed Implementation Guide

1. Model request aggregate with optimistic locking (`@Version`).
2. Add Flyway migrations:
   - request table
   - request status history (optional but recommended)
   - indexes by status/date/type/requestor
3. Define API DTOs and validation rules for required fields and enum domains.
4. Implement request application service with explicit transition methods.
5. Enforce transition guards and return clear domain error codes for invalid transitions.
6. Implement submission flow:
   - load active workflow version for request type
   - persist version binding
   - emit domain event and audit event in same transaction
7. Implement list/filter endpoint with pagination and sortable fields.
8. Add tests:
   - valid transitions
   - invalid transitions rejected
   - concurrent update conflict handling

## Deliverables

- Request domain model and persistence.
- Complete request REST endpoints.
- Transition and validation test coverage.

## Acceptance Criteria

- Request cannot be edited outside `DRAFT` and `CHANGES_REQUESTED`.
- Submit operation binds to a specific active workflow version.
- Duplicate submit call with same idempotency key does not create duplicate workflow runtime.
- List endpoint supports pagination and filters without full table scans.
- API returns consistent error payload for invalid transitions.
