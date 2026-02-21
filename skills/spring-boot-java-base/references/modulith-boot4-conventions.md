# Spring Modulith and Boot 4 Conventions

## Table of Contents

1. Boot 4 Baseline
2. Boot 4 Starter Conventions
3. Boot 4 Test Starter Conventions
4. Spring Modulith Core Setup
5. Compatibility Guardrail
6. Build Snippets

## 1. Boot 4 Baseline

- Target Java 17 or later.
- Align with Jakarta EE 11 APIs and Servlet 6.1 containers.
- Align Spring Framework usage to the Boot-managed 7.x line.
- Prefer supported servers for Boot 4 workloads (Tomcat, Jetty, or Reactor Netty).

## 2. Boot 4 Starter Conventions

- Use `spring-boot-starter-<technology>` for main dependencies.
- Keep direct library coordinates only for libraries without an official starter.
- Replace:
  - `org.flywaydb:flyway-*` with `org.springframework.boot:spring-boot-starter-flyway`
  - `org.liquibase:liquibase-core` with `org.springframework.boot:spring-boot-starter-liquibase`

## 3. Boot 4 Test Starter Conventions

- Use `spring-boot-starter-<technology>-test` for technology-specific test utilities.
- Add `org.springframework.boot:spring-boot-starter-security-test` when using security test annotations/utilities.

## 4. Spring Modulith Core Setup

- Import the Modulith BOM first, then add `spring-modulith-starter-core`.
- Treat `spring-modulith-starter-core` as the baseline bundle for Modulith API, module model/runtime support, and moments integration.
- Add only domain-specific Modulith starters when needed:
  - `spring-modulith-starter-jpa`
  - `spring-modulith-starter-jdbc`
  - `spring-modulith-starter-jooq`
  - `spring-modulith-starter-mongodb`
  - `spring-modulith-starter-neo4j`
- Add `spring-modulith-starter-test` for module-focused tests.
- Enable runtime verification when module dependency checks are required:
  - `spring.modulith.runtime.verification-enabled=true`

## 5. Compatibility Guardrail

- Check the official Spring Modulith compatibility matrix before pinning versions.
- Keep Spring Boot and Modulith versions matched through managed BOM versions; avoid unmanaged mix-and-match upgrades.

## 6. Build Snippets

### Gradle

```gradle
dependencies {
    implementation platform("org.springframework.modulith:spring-modulith-bom:${modulithVersion}")
    implementation "org.springframework.modulith:spring-modulith-starter-core"

    testImplementation "org.springframework.modulith:spring-modulith-starter-test"
}
```

### Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-bom</artifactId>
      <version>${spring-modulith.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```
