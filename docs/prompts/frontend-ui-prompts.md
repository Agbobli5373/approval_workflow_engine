# Frontend UI Prompts (Based on Epics E0-E11)

## 1) Master Frontend Build Prompt

```text
You are a senior UX designer and frontend engineer.

Design and implement a production-ready frontend UI for an "Approval Workflow Engine" based on epics E0-E11.
Tech stack: React + TypeScript + React Router + React Query + Tailwind + shadcn/ui.
Backend contract: REST under `/api`, optional header `API-Version: 1.0`, JWT auth, standard error payload `{ code, message, correlationId, details }`.

Primary roles:
1. REQUESTOR
2. APPROVER
3. WORKFLOW_ADMIN

Build these core screens and flows:

1. Authentication
- Login, logout, "me" profile.
- Role-aware navigation.
- Handle 401, 403, 429 with clear UX and correlation ID visibility.

2. Request Lifecycle
- Create/edit request.
- Statuses: DRAFT, SUBMITTED, IN_REVIEW, CHANGES_REQUESTED, APPROVED, REJECTED, CANCELLED, EXPIRED.
- Enforce UI guardrails:
  - Edit only DRAFT/CHANGES_REQUESTED.
  - Submit only DRAFT/CHANGES_REQUESTED.
  - Cancel only DRAFT/SUBMITTED/IN_REVIEW/CHANGES_REQUESTED.
- Request list with pagination, filter, sort, and request details page.
- Use idempotency keys for submit/cancel.

3. Task Inbox + Decisions
- Task list filter (`assignedTo=me`, status).
- Task statuses: PENDING, CLAIMED, APPROVED, REJECTED, CANCELLED, EXPIRED, SKIPPED.
- Claim task.
- Decision actions: APPROVE, REJECT, SEND_BACK, DELEGATE.
- Require comment for REJECT and SEND_BACK.
- Show optimistic locking/idempotent retry UX.

4. Workflow Template Admin
- Create workflow definitions and versions.
- Version lifecycle: DRAFT, ACTIVE, RETIRED.
- Activate/retire flows.
- Active versions immutable in UI.
- Include graph visualization for steps/edges/gateways/joins.

5. Rules Engine Admin
- JSON DSL editor for conditions.
- Operators: all, any, not, ==, !=, >, >=, <, <=, in, contains, matches.
- "Simulate/Explain" mode with readable evaluation trace.

6. Delegation
- Create/update/revoke delegation policy (delegator, delegatee, validity window, scope).
- Display "Acted on behalf of" badge on delegated decisions.

7. SLA + Escalation Monitoring
- Due dates, escalation stages, overdue indicators.
- Admin view for scheduler lag/failures and escalation history.

8. Notifications
- Notification center for task assigned, SLA risk, request approved/rejected.
- Delivery status and retry/dead-letter visibility (admin).

9. Audit & Compliance
- Request audit timeline with actor, timestamp, action, workflow version, details.
- Tamper-evidence status indicator (hash-chain verification result).

10. Integrations/Webhooks
- Webhook subscription create/list/status controls.
- Event delivery visibility and outbox backlog indicator.

11. Observability
- Operational dashboard: decision counts, decision latency, SLA breaches, outbox backlog.
- Health status cards: app, DB, scheduler, outbox publisher.

UX/UI requirements:
- Responsive desktop + mobile.
- Accessible (keyboard, ARIA, contrast).
- Clear visual status system for request/task states.
- Avoid generic styling; use a strong, intentional visual direction (custom type scale, structured spacing, meaningful motion).
- Consistent loading/empty/error/success states.
- Safe destructive-action confirmations.

Implementation requirements:
- Route-level auth guards by role.
- Typed API client layer.
- Centralized error handling using correlationId.
- Reusable table, filter bar, status badge, timeline, and decision modal components.
- Feature-based folder structure.

Now produce:
1. Information architecture + route map.
2. Page-by-page wireframe spec.
3. Component inventory.
4. State/data flow plan.
5. Design tokens (color, typography, spacing, motion).
6. Starter React code for key pages (Auth, Requests list/detail, Task inbox, Workflow admin, Audit timeline).
```

## 2) Figma Design Prompt

```text
Act as a principal product designer. Create a complete UI design system and high-fidelity app screens in Figma for an "Approval Workflow Engine" (enterprise SaaS).

Goal:
Design-only output (no code). Build realistic, implementation-ready desktop and mobile UI covering epics E0-E11.

Product context:
- Roles: REQUESTOR, APPROVER, WORKFLOW_ADMIN
- API style: REST `/api`, JWT auth, API-Version header, error shape `{ code, message, correlationId, details }`
- Core domains: requests, tasks, workflow templates, rules DSL, delegation, SLA/escalations, notifications, audit ledger, webhooks/outbox, observability.

Required screens:
1. Auth: Login, Logout, Profile/Me.
2. Global app shell: role-aware sidebar/topbar, search, alerts.
3. Requests:
   - Requests list (filters, sort, pagination)
   - Request create/edit
   - Request details + lifecycle timeline
   - Statuses: DRAFT, SUBMITTED, IN_REVIEW, CHANGES_REQUESTED, APPROVED, REJECTED, CANCELLED, EXPIRED
4. Tasks:
   - Task inbox (`assignedTo=me`, status filters)
   - Task details with claim + decision modal
   - Actions: APPROVE, REJECT, SEND_BACK, DELEGATE
   - Task statuses: PENDING, CLAIMED, APPROVED, REJECTED, CANCELLED, EXPIRED, SKIPPED
5. Workflow Admin:
   - Workflow definitions list
   - Version management (DRAFT, ACTIVE, RETIRED)
   - Graph editor/visualizer for steps, edges, gateways, joins
6. Rules Engine:
   - JSON DSL editor screen
   - Rule simulation/explain results
7. Delegation:
   - Create/update/revoke policy
   - "Acted on behalf of" indicators in task/audit UI
8. SLA & Escalations:
   - Overdue queue, escalation stage tracker, scheduler health cues
9. Notifications:
   - Notification center and delivery state views
   - Admin retry/dead-letter monitoring
10. Audit & Compliance:
   - Request audit timeline with actor, timestamp, action, workflow version
   - Tamper-evidence/hash-chain status widget
11. Integrations:
   - Webhook subscriptions create/list/status
   - Outbox backlog and delivery health
12. Observability dashboard:
   - Decision counts, latency, SLA breaches, outbox backlog
   - Health cards: app, DB, scheduler, outbox publisher

UX rules:
- Strict role-based visibility and action permissions.
- Clear status badges and transition affordances.
- Strong empty/loading/error states including correlationId display in error surfaces.
- Accessible design (contrast, keyboard flow, focus states, ARIA-ready patterns).
- Responsive layouts for desktop and mobile.
- Enterprise-ready visual language: deliberate typography, spacing scale, color tokens, purposeful motion.
- Avoid generic templates; create a distinctive but practical B2B interface.

Deliverables in Figma:
1. Sitemap + user flows for all 3 roles.
2. Design tokens (color, type, spacing, radius, shadows, motion).
3. Component library with variants:
   - Buttons, inputs, selects, tables, filter bar, badges, tabs, cards, modals, drawers, toasts, timeline, stepper, status chips.
4. High-fidelity screens for all required pages (desktop + key mobile breakpoints).
5. Clickable prototype for 3 critical flows:
   - Request creation -> submit
   - Task claim -> decision
   - Admin workflow version activation
6. Handoff notes:
   - Interaction behavior, validation rules, state logic, and edge cases per screen.

Produce organized Figma pages:
- 00 Foundations
- 01 Components
- 02 Requestor Flows
- 03 Approver Flows
- 04 Admin Flows
- 05 Prototype
- 06 Handoff Notes
```
