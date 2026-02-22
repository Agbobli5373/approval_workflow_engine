# Rules Controller Test Data Guide (Epic 4)

This guide provides ready payloads for:
- Rules APIs (`/api/rule-sets/*`)
- Workflow activation checks that validate `GATEWAY.ruleRef` against DB rulesets

Payload files are in:
- `docs/test-data/rules-controller/`

## 1. Authenticate (local/test profile)

Seeded admin user:
- username: `admin`
- password: `password`

```bash
BASE_URL=http://localhost:8080

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "API-Version: 1.0" \
  -d '{"usernameOrEmail":"admin","password":"password"}' | jq -r '.accessToken')
```

## 2. Create ruleset versions

### v1 (valid)

```bash
curl -i -X POST "$BASE_URL/api/rule-sets/EXPENSE_ROUTING/versions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/rules-controller/create-rule-set-version-expense-routing-v1.json
```

Expected: `201 Created`, `versionNo = 1`, non-empty `checksumSha256`.

### v2 (valid)

```bash
curl -i -X POST "$BASE_URL/api/rule-sets/EXPENSE_ROUTING/versions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/rules-controller/create-rule-set-version-expense-routing-v2.json
```

Expected: `201 Created`, `versionNo = 2`.

### invalid operator (negative)

```bash
curl -i -X POST "$BASE_URL/api/rule-sets/EXPENSE_ROUTING/versions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/rules-controller/create-rule-set-version-invalid-operator.json
```

Expected: `400 Bad Request`, `ApiError.code = BAD_REQUEST`.

### invalid regex (negative)

```bash
curl -i -X POST "$BASE_URL/api/rule-sets/EXPENSE_ROUTING/versions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/rules-controller/create-rule-set-version-invalid-regex.json
```

Expected: `400 Bad Request` due to regex guardrails.

## 3. Read/list versions

```bash
curl -i -X GET "$BASE_URL/api/rule-sets/EXPENSE_ROUTING/versions/1" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0"

curl -i -X GET "$BASE_URL/api/rule-sets/EXPENSE_ROUTING/versions?page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0"
```

## 4. Simulate evaluation

### matching context

```bash
curl -i -X POST "$BASE_URL/api/rule-sets/simulations" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/rules-controller/simulate-rule-set-expense-routing-match.json
```

Expected: `200 OK`, `matched = true`, with traces.

### non-matching context

```bash
curl -i -X POST "$BASE_URL/api/rule-sets/simulations" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/rules-controller/simulate-rule-set-expense-routing-no-match.json
```

Expected: `200 OK`, `matched = false`.

## 5. Workflow activation integration check

After creating `EXPENSE_ROUTING` version `1`, you can verify activation-time ruleRef enforcement.

### valid ruleRef

```bash
curl -i -X POST "$BASE_URL/api/workflow-definitions/WF_RULE_REF_TEST/versions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/rules-controller/workflow-gateway-rule-ref-valid.json
```

Expected: draft version created; activation should succeed.

### missing ruleRef

```bash
curl -i -X POST "$BASE_URL/api/workflow-definitions/WF_RULE_REF_TEST/versions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "API-Version: 1.0" \
  -H "Content-Type: application/json" \
  -d @docs/test-data/rules-controller/workflow-gateway-rule-ref-missing.json
```

Expected: draft version can be created; activation fails with `409 Conflict` due to missing ruleset/version.
