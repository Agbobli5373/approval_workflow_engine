# E4: Rules Engine (JSON DSL)

## Epic Goal

Provide deterministic, versioned rule evaluation for routing and gateway conditions, with admin APIs and activation-time `ruleRef` enforcement.

## Implemented Features

- DB-backed ruleset versioning (`rule_sets`) with immutable versions.
- JSON DSL parser and validator using explicit AST node shapes:
  - logical: `all`, `any`, `not`
  - predicate: `field`, `op`, `value`
- Supported operators:
  - `==`, `!=`, `>`, `>=`, `<`, `<=`, `in`, `contains`, `matches`
- Deterministic evaluation semantics over request context fields:
  - `amount`, `department`, `requestType`, `currency`, `payload.*`
- Regex safety guardrails for `matches`:
  - max pattern length
  - max input length
  - reject lookbehind and backreference constructs
- Ruleset checksum on canonical DSL JSON (`SHA-256`).
- Admin APIs under `/api`:
  - `POST /api/rule-sets/{ruleSetKey}/versions`
  - `GET /api/rule-sets/{ruleSetKey}/versions/{versionNo}`
  - `GET /api/rule-sets/{ruleSetKey}/versions`
  - `POST /api/rule-sets/simulations`
- Workflow-template integration:
  - activation now verifies each `GATEWAY.ruleRef` references an existing ruleset key/version.

## Implementation Artifacts

- Migrations:
  - `src/main/resources/db/migration/postgresql/V6__rulesets.sql`
  - `src/main/resources/db/migration/h2/V6__rulesets.sql`
- Rules API/service:
  - `src/main/java/com/isaac/approvalworkflowengine/rules/api/RuleSetController.java`
  - `src/main/java/com/isaac/approvalworkflowengine/rules/service/RuleSetService.java`
  - `src/main/java/com/isaac/approvalworkflowengine/rules/RuleSetLookup.java`
- DSL and evaluation:
  - `src/main/java/com/isaac/approvalworkflowengine/rules/dsl/RuleDslParser.java`
  - `src/main/java/com/isaac/approvalworkflowengine/rules/evaluation/RuleEvaluator.java`
  - `src/main/java/com/isaac/approvalworkflowengine/rules/evaluation/RuleFieldResolver.java`
  - `src/main/java/com/isaac/approvalworkflowengine/rules/validation/RuleRegexGuard.java`
  - `src/main/java/com/isaac/approvalworkflowengine/rules/checksum/RuleDslChecksumService.java`
- Persistence:
  - `src/main/java/com/isaac/approvalworkflowengine/rules/repository/entity/RuleSetEntity.java`
  - `src/main/java/com/isaac/approvalworkflowengine/rules/repository/RuleSetJpaRepository.java`
- Workflow activation integration:
  - `src/main/java/com/isaac/approvalworkflowengine/workflowtemplate/service/WorkflowTemplateService.java`

## Tests Added/Updated

- Rules unit tests:
  - `src/test/java/com/isaac/approvalworkflowengine/rules/RuleDslParserTest.java`
  - `src/test/java/com/isaac/approvalworkflowengine/rules/RuleEvaluatorTest.java`
- Rules API integration tests:
  - `src/test/java/com/isaac/approvalworkflowengine/rules/RuleSetApiTest.java`
- Workflow template integration updates:
  - `src/test/java/com/isaac/approvalworkflowengine/workflowtemplate/WorkflowTemplateApiTest.java`
- Migration smoke updates:
  - `src/test/java/com/isaac/approvalworkflowengine/platform/MigrationSmokeTest.java`

## Acceptance Criteria

- All required DSL operators evaluate correctly with tests.
- Same rule version and same context produce deterministic output.
- Invalid DSL is rejected with `400` and structured error details.
- Gateway `ruleRef` to missing ruleset/version blocks workflow activation with conflict.
- Rules APIs are admin-only and integrated into OpenAPI contract.

## Deferred To Later Epics

- Runtime gateway task execution wiring (E5).
- Audit append/hash-chain coverage for rules events (E9).
- Outbox/integration event publishing for rules lifecycle (E10).
