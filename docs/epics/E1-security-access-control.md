# E1: Security and Access Control

## Epic Goal

Implement robust authentication and authorization so only valid actors can perform allowed actions across requests, tasks, workflows, and audit endpoints.

## Features To Implement

- OAuth2/OIDC JWT resource server configuration.
- Role model and RBAC mapping for key personas.
- ABAC policy evaluation for action-level checks.
- Endpoint security policy by module.
- Rate limiting strategy (user/client level).
- Security-safe error handling with no sensitive detail leaks.

## Detailed Implementation Guide

1. Configure JWT resource server and token claim mapping (`sub`, roles/scopes, tenant if present).
2. Define role taxonomy:
   - `REQUESTOR`
   - `APPROVER`
   - `WORKFLOW_ADMIN`
   - `AUDITOR`
   - `INTEGRATOR`
3. Implement endpoint RBAC through `SecurityFilterChain` and method-level guards.
4. Implement ABAC policies in domain/application layer (assignment match, delegation scope, department boundaries).
5. Add policy utility for "can decide task", "can edit request", "can activate workflow" checks.
6. Add rate limiting integration (in-memory for local, Redis-ready extension for production).
7. Standardize security audit logs for denied decisions and privileged actions.
8. Add tests:
   - authentication required for protected APIs
   - role mismatch returns 403
   - ABAC policy mismatch returns 403 even with valid role

## Deliverables

- Security configuration and policy library.
- Authenticated principal model used across modules.
- Security-focused test suite for core authorization paths.

## Acceptance Criteria

- All non-public endpoints reject anonymous requests with 401.
- RBAC restrictions enforced for every route family.
- ABAC checks prevent cross-department or unassigned decisions.
- Security tests cover positive and negative policy cases.
- No security exception leaks stack traces or internal schema.
