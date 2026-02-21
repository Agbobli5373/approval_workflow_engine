# Spring Boot Base Patterns

## Table of Contents

1. Package Layout
2. API Contract Checklist
3. Persistence and Transaction Checklist
4. Security Baseline
5. Error Handling Pattern
6. Testing Matrix
7. Gradle Commands

## 1. Package Layout

Use a feature-first layout and keep shared concerns in `common`.

```text
src/main/java/com/example/app/
  common/
    config/
    exception/
    logging/
  <feature>/
    controller/
    dto/
    model/
    repository/
    service/
```

## 2. API Contract Checklist

- Define request and response DTOs first.
- Keep endpoint paths resource-oriented (`/api/v1/orders/{id}`).
- Return stable response fields and avoid exposing entity internals.
- Add pagination and sorting for list endpoints.
- Validate request DTOs and map violations to consistent error payloads.

## 3. Persistence and Transaction Checklist

- Add schema migration before writing business logic.
- Keep write operations inside service-layer transactions.
- Use `@Version` for optimistic locking on mutable entities.
- Avoid N+1 queries with fetch joins, entity graphs, or explicit query methods.
- Keep repository interfaces minimal; move orchestration to services.

## 4. Security Baseline

- Configure `SecurityFilterChain` explicitly.
- Default to authenticated endpoints and open only required public routes.
- Use method-level checks (`@PreAuthorize`) for role/ownership rules.
- Never trust client-provided actor or tenant identifiers.

## 5. Error Handling Pattern

Use a centralized exception handler and deterministic error shape.

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(EntityNotFoundException ex) {
        return new ApiError("NOT_FOUND", ex.getMessage());
    }
}
```

```java
public record ApiError(String code, String message) {}
```

## 6. Testing Matrix

- Unit tests: service rules, validators, mappers.
- `@DataJpaTest`: repository query correctness.
- `@WebMvcTest`: controller validation and status mapping.
- `@SpringBootTest`: end-to-end behavior for critical flows.
- Testcontainers: PostgreSQL-specific behavior and migrations.

## 7. Gradle Commands

```bash
./gradlew clean test
./gradlew test --tests "*ServiceTest"
./gradlew bootRun
```
