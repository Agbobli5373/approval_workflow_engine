# Controller Test Data Guide (Request + Workflow Template)

This guide gives you ready test data and command flows for:
- `/api/requests` (`RequestController`)
- `/api/workflow-definitions/*` and `/api/workflow-versions/*` (`WorkflowTemplateController`)

All payload files referenced below are in:
- `docs/test-data/request-controller/`
- `docs/test-data/workflow-template-controller/`

## 1. Local Seed Users and Credentials

In `LOCAL_AUTH` mode (default local profile), seeded users are:
- `admin` (`WORKFLOW_ADMIN`)
- `requestor` (`REQUESTOR`)
- `approver` (`APPROVER`)

Default password in local/test is `password` (unless you override Flyway placeholder env vars).

## 2. Known Seeded Workflow Template (already in DB)

On local/test startup, this active template is pre-seeded:
- `definitionKey`: `EXPENSE_DEFAULT`
- `requestType`: `EXPENSE`
- `active workflowVersionId`: `11111111-1111-1111-1111-111111111111`

This lets request submit work immediately for `requestType=EXPENSE`.

## 3. Common Headers

Use these headers in requests:
- `Authorization: Bearer <token>`
- `API-Version: 1.0`
- `Content-Type: application/json`

Base URL assumed below:
- `http://localhost:8080`

## 4. Authenticate and Extract Tokens

```bash
BASE_URL=http://localhost:8080

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "API-Version: 1.0" \
  -d '{"usernameOrEmail":"admin","password":"password"}' | jq -r '.accessToken')

REQUESTOR_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "API-Version: 1.0" \
  -d '{"usernameOrEmail":"requestor","password":"password"}' | jq -r '.accessToken')
```

If `jq` is not installed, call login once and copy `accessToken` manually.

## 5. WorkflowTemplateController Test Data and Flows

### 5.1 Create workflow definition (admin only)

Payload file:
- `docs/test-data/workflow-template-controller/create-definition-procurement.json`

```bash
curl -i -X POST "$BASE_URL/api/workflow-definitions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/workflow-template-controller/create-definition-procurement.json
```

Expected: `201 Created`.

### 5.2 Create workflow version (DRAFT)

Payload file:
- `docs/test-data/workflow-template-controller/create-version-procurement-valid.json`

```bash
VERSION_ID=$(curl -s -X POST "$BASE_URL/api/workflow-definitions/PROCUREMENT_STANDARD/versions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/workflow-template-controller/create-version-procurement-valid.json | jq -r '.id')

echo "$VERSION_ID"
```

Expected: `201 Created`, `status = DRAFT`.

### 5.3 Activate workflow version

```bash
curl -i -X POST "$BASE_URL/api/workflow-versions/$VERSION_ID/activate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0"
```

Expected: `200 OK`, `status = ACTIVE`, `checksumSha256` is populated.

### 5.4 Get workflow version

```bash
curl -i -X GET "$BASE_URL/api/workflow-versions/$VERSION_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0"
```

Expected: `200 OK` with graph and status.

### 5.5 Negative graph validation test (dangling edge)

Payload file:
- `docs/test-data/workflow-template-controller/create-version-invalid-dangling-edge.json`

```bash
BAD_VERSION_ID=$(curl -s -X POST "$BASE_URL/api/workflow-definitions/PROCUREMENT_STANDARD/versions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/workflow-template-controller/create-version-invalid-dangling-edge.json | jq -r '.id')

curl -i -X POST "$BASE_URL/api/workflow-versions/$BAD_VERSION_ID/activate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0"
```

Expected: `409 Conflict` with `ApiError.code = CONFLICT`.

### 5.6 Negative graph validation test (cycle, loopback disabled)

Payload file:
- `docs/test-data/workflow-template-controller/create-version-invalid-cycle.json`

Run the same create/activate flow; activation should return `409 Conflict`.

## 6. RequestController Test Data and Flows

### 6.1 Create request (requestor)

Payload file:
- `docs/test-data/request-controller/create-request-expense.json`

```bash
REQUEST_ID=$(curl -s -X POST "$BASE_URL/api/requests" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/request-controller/create-request-expense.json | jq -r '.id')

echo "$REQUEST_ID"
```

Expected: `201 Created`, `status = DRAFT`.

### 6.2 Update request

Payload file:
- `docs/test-data/request-controller/update-request-expense.json`

```bash
curl -i -X PATCH "$BASE_URL/api/requests/$REQUEST_ID" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/request-controller/update-request-expense.json
```

Expected: `200 OK`, still editable while `DRAFT`.

### 6.3 Submit request (idempotent)

```bash
curl -i -X POST "$BASE_URL/api/requests/$REQUEST_ID/submit" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Idempotency-Key: submit-req-001"
```

Expected: `202 Accepted`, `status = SUBMITTED`, `workflowVersionId` bound from active `EXPENSE` template.

### 6.4 Repeat submit with same idempotency key

```bash
curl -i -X POST "$BASE_URL/api/requests/$REQUEST_ID/submit" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Idempotency-Key: submit-req-001"
```

Expected: deterministic result, no duplicate transition.

### 6.5 Cancel request (allowed from SUBMITTED)

```bash
curl -i -X POST "$BASE_URL/api/requests/$REQUEST_ID/cancel" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Idempotency-Key: cancel-req-001"
```

Expected: `200 OK`, `status = CANCELLED`.

### 6.6 Read and list requests

```bash
curl -i -X GET "$BASE_URL/api/requests/$REQUEST_ID" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0"

curl -i -X GET "$BASE_URL/api/requests?status=CANCELLED&page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0"
```

Expected: `200 OK`.

### 6.7 Validation failure sample

Payload file:
- `docs/test-data/request-controller/create-request-invalid.json`

```bash
curl -i -X POST "$BASE_URL/api/requests" \
  -H "Authorization: Bearer $REQUESTOR_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/request-controller/create-request-invalid.json
```

Expected: `400 Bad Request` with `ApiError.code = VALIDATION_ERROR`.

## 7. Controller Ownership/Role Checks You Can Verify

- `WorkflowTemplateController` endpoints require admin role (`WORKFLOW_ADMIN`); call with `requestor` token to get `403`.
- `RequestController` allows authenticated users, but non-admin users can only read/update their own requests.

