---
name: spring-boot-java-base
description: Build and evolve Java Spring Boot 4 backend features with clean architecture, REST APIs, validation, security, persistence, testing, and Spring Modulith foundations. Use when scaffolding or refactoring Spring Boot services/controllers/entities/repositories, aligning dependencies with Boot 4 starter/module conventions, or introducing application module boundaries with spring-modulith-starter-core.
---

# Spring Boot Java Base

Implement baseline Spring Boot backend changes with Boot 4 and Modulith guardrails.

## Execution Flow

1. Translate the task into five parts: API contract, domain rules, persistence, authorization, and module boundary.
2. Implement in this order:
   - Model + database migration
   - Service/use-case logic + transaction boundary
   - Controller + request/response DTO + validation
   - Security/authorization checks
   - Tests
3. Keep controllers thin and move business logic into services.
4. Organize code by feature (`<feature>/controller`, `<feature>/service`, `<feature>/repository`, `<feature>/model`, `<feature>/dto`).
5. Define module boundaries early for medium or large features (export named interfaces and keep internals package-private).

## Dependency Baseline (Boot 4 + Modulith)

- Use Spring Boot starters for both main and test dependencies.
- Apply Spring Boot 4 starter conventions (`spring-boot-starter-<technology>` and `spring-boot-starter-<technology>-test`).
- Replace direct Flyway/Liquibase dependencies with `spring-boot-starter-flyway` or `spring-boot-starter-liquibase`.
- Import the Spring Modulith BOM and add `spring-modulith-starter-core` for module model/runtime support.
- Add `spring-modulith-starter-test` when writing module integration tests.
- Verify Spring Modulith and Spring Boot compatibility in the official matrix before pinning versions.

## Required Practices

- Use constructor injection only.
- Validate inbound DTOs with `jakarta.validation` annotations and `@Valid`.
- Keep entities separate from API DTOs.
- Use explicit transaction boundaries in service methods.
- Add centralized exception mapping with `@RestControllerAdvice`.
- Prefer optimistic locking (`@Version`) for mutable aggregate roots.
- Add pagination for list endpoints by default.
- Use structured logs that include request/correlation identifiers.
- Target Spring Boot 4 baselines: Java 17+, Jakarta EE 11, Servlet 6.1, Spring Framework 7.x.

## Testing Standard

- Add unit tests for service decisions and edge cases.
- Add slice/integration tests for persistence and controller behavior.
- Cover success and failure paths: validation errors, not-found, conflict, and forbidden.
- Use Testcontainers for database-sensitive integration tests when SQL behavior matters.
- Use technology-specific Boot 4 test starters (for example, `spring-boot-starter-security-test` for security test utilities).
- Use `spring-modulith-starter-test` for application-module verification and integration tests.

## Resource Loading

- Load `references/base-patterns.md` for reusable endpoint, persistence, security, and testing checklists before implementing medium or large backend changes.
- Load `references/modulith-boot4-conventions.md` before changing dependencies, migration rules, or module boundaries.
