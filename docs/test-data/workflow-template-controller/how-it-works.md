# How `WorkflowTemplateController` Works

`WorkflowTemplateController` is an admin-only API façade over `WorkflowTemplateService`.

## 1. Security and Routing

- Base mapping: `/api` with API version `1.0`.
- Class-level authorization: `@PreAuthorize("hasRole('WORKFLOW_ADMIN')")`.
- Result: all four endpoints require a JWT with `ROLE_WORKFLOW_ADMIN`.

Endpoints:
- `POST /api/workflow-definitions`
- `POST /api/workflow-definitions/{definitionKey}/versions`
- `POST /api/workflow-versions/{workflowVersionId}/activate`
- `GET /api/workflow-versions/{workflowVersionId}`

## 2. Create Definition

Input (`WorkflowDefinitionInput`):
- `definitionKey`
- `name`
- `requestType`
- `allowLoopback`

Behavior in service:
- Normalizes `definitionKey` and `requestType` to uppercase.
- Enforces uniqueness on `definitionKey` and `requestType`.
- Persists a `workflow_definitions` row with owner from authenticated actor.

## 3. Create Version

Input (`WorkflowVersionInput`):
- `graph` object (`nodes`, `edges`, optional `policies`)

Behavior in service:
- Resolves workflow definition by key.
- Computes `versionNo = max + 1`.
- Stores version as `DRAFT` in `workflow_versions` with `graph_json` snapshot.
- Also stores normalized graph shape in `workflow_nodes` and `workflow_edges`.

## 4. Activate Version

Activation is transactional and guarded:
- Loads target version with lock (`findForUpdate`).
- Rejects activation unless status is `DRAFT`.
- Loads graph from `graph_json` and validates it:
  - exactly one `START` and one `END`
  - no dangling edges
  - all nodes reachable from `START`
  - all nodes can reach `END`
  - non-terminal nodes have outgoing edges
  - cycle blocked when `allowLoopback=false`
  - node-specific validation for `APPROVAL`, `GATEWAY`, `JOIN`
- Canonicalizes graph JSON deterministically.
- Computes and persists SHA-256 checksum (`checksum_sha256`).
- Marks target version `ACTIVE`.
- Auto-retires any previous `ACTIVE` version for the same definition.

## 5. Get Version

- Reads version by id.
- Resolves parent definition.
- Returns:
  - `id`
  - `definitionKey`
  - `versionNo`
  - `status`
  - `graph`
  - `checksumSha256`
  - `activatedAt`

## 6. Error Model

Controller uses global exception mapping:
- validation errors -> `400` (`VALIDATION_ERROR`)
- missing resources -> `404` (`NOT_FOUND`)
- state/graph conflicts -> `409` (`CONFLICT`)
- auth failures -> `401`/`403`

All error responses follow `ApiError { code, message, correlationId, details[] }`.
