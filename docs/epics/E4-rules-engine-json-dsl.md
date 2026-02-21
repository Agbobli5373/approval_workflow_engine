# E4: Rules Engine (JSON DSL)

## Epic Goal

Provide deterministic, versioned rule evaluation for routing and gateway conditions across workflow templates and runtime execution.

## Features To Implement

- JSON DSL schema for conditions.
- Operators support:
  - combinators: `all`, `any`, `not`
  - comparisons: `==`, `!=`, `>`, `>=`, `<`, `<=`, `in`, `contains`, `matches`
- Request-context field resolution (`amount`, `department`, `requestType`, payload fields).
- Rule versioning with reproducible evaluation.
- Explain/debug mode for rule simulation results (internal/admin use).

## Detailed Implementation Guide

1. Define DSL JSON schema and validation rules (required keys, type contracts, supported operators).
2. Implement parser that converts DSL JSON into a safe internal AST.
3. Implement evaluator with strict deterministic semantics:
   - fixed operator precedence
   - stable list ordering
   - no side effects
4. Implement field accessor strategy for request context and nested payload paths.
5. Add regex safety guardrails for `matches` to prevent pathological patterns.
6. Implement rule version storage and version references from workflow templates.
7. Add simulation service to test rule outcomes against sample context data.
8. Add tests:
   - operator correctness
   - null/missing field behavior
   - deterministic re-evaluation with same inputs

## Deliverables

- Rules DSL parser/evaluator library.
- Rule version persistence and lookup.
- Rule simulation endpoint/service for admins.

## Acceptance Criteria

- All required operators evaluate correctly with test coverage.
- Same rule version and same input produce identical output across runs.
- Invalid DSL structures are rejected with clear validation errors.
- Runtime workflow routing can call evaluator without unsafe reflection or dynamic code execution.
- Rule evaluation failures are observable via logs/metrics and do not crash worker loops.
