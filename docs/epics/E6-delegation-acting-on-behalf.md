# E6: Delegation and Acting-on-Behalf

## Epic Goal

Allow approvers to delegate decisions safely within controlled scope and time windows while preserving accountability.

## Features To Implement

- Delegation policy model:
  - delegator
  - delegatee
  - validity window
  - scope (request type, department, role, or all)
- Delegation creation/update/revoke APIs (admin/self policy-driven).
- Runtime delegation resolution during task assignment and decision authorization.
- "Acted on behalf of" metadata on task decisions and audit records.

## Detailed Implementation Guide

1. Add delegation tables and indexes by delegator/delegatee/time window.
2. Implement delegation policy validation:
   - non-overlapping conflicts per policy choice
   - valid date window
   - scope integrity
3. Implement resolver that maps actionable task to effective actor chain.
4. Update ABAC checks to accept delegate decisions only when policy scope matches task context.
5. Record both `actorId` and `actedOnBehalfOfId` on decision records.
6. Emit audit events with delegation metadata.
7. Add read endpoint/query support for active delegation policies.
8. Add tests for:
   - valid delegated approval
   - expired delegation rejection
   - out-of-scope delegation rejection

## Deliverables

- Delegation domain model and APIs.
- Runtime authorization integration for delegation.
- Audit/event enrichment for delegated actions.

## Acceptance Criteria

- Delegated decisions are accepted only inside active scope and time window.
- All delegated actions record both acting and represented user identity.
- Unauthorized delegate attempts return 403 and generate security/audit signal.
- Revoked delegation is effective immediately for new decisions.
- Delegation tests cover scope boundaries and expiration edge cases.
