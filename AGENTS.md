# Repository Guidelines

## Project Structure & Module Organization

Med Clinic is a planned Spring Boot monolith with server-rendered Thymeleaf pages for medical clinic management. Currently, the repository contains Spec Kit configuration in `.specify/` and workflow skills in `.agents/skills/`; application code and build files have not been created.

When scaffolding the application, use `src/main/java/` for Java, `src/main/resources/templates/` for Thymeleaf, `src/main/resources/static/` for CSS, JavaScript, and images, and `src/test/java/` for tests. Separate domain, application, infrastructure, and presentation responsibilities. Keep business rules independent of Spring and persistence through internal contracts and adapters.

## Build, Test, and Development Commands

No build or test commands are executable yet. Once a build tool is selected, commit its wrapper and document the supported commands. For Maven, use `./mvnw spring-boot:run` to run locally, `./mvnw test` for unit tests, and `./mvnw verify` for configured verification checks. On Windows PowerShell, use `.\mvnw.cmd` instead of `./mvnw`. These commands require the future Maven scaffold and corresponding plugin configuration.

## Coding Style & Naming Conventions

Use four-space indentation in Java, lowercase package names, `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Prefer descriptive domain names and focused classes. Reuse Thymeleaf fragments for shared interface elements. No formatter or linter is configured yet; select and configure both with the application scaffold.

## Testing Guidelines

Follow constitution 2.0.0 and the user's demo scope: a few essential tests for critical business rules, permissions and transactions, written before critical behavior changes. There is no minimum coverage percentage or mandatory load/browser suite. Use fictitious patients only and complement automated checks with a short manual demonstration script.

## Commit & Pull Request Guidelines

Use Conventional Commits. Commit each completed task individually, as requested by the user. Explain behavior changes and actual validation. No separate role approvals, document hashes or formal accessibility certificates are required for the demo MVP.

## Security & Configuration

Keep credentials, patient data, and private keys out of version control and logs. Supply secrets externally and use fictitious example values. Keep wrappers, shared application configuration, and database migrations versioned; store local overrides in ignored `application-local.*` files.
