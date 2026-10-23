# Repository Guidelines

## Project Structure & Module Organization

Med Clinic is a planned Spring Boot monolith with server-rendered Thymeleaf pages for medical clinic management. Currently, the repository contains Spec Kit configuration in `.specify/` and workflow skills in `.agents/skills/`; application code and build files have not been created.

When scaffolding the application, use `src/main/java/` for Java, `src/main/resources/templates/` for Thymeleaf, `src/main/resources/static/` for CSS, JavaScript, and images, and `src/test/java/` for tests. Separate domain, application, infrastructure, and presentation responsibilities. Keep business rules independent of Spring and persistence through internal contracts and adapters.

## Build, Test, and Development Commands

No build or test commands are executable yet. Once a build tool is selected, commit its wrapper and document the supported commands. For Maven, use `./mvnw spring-boot:run` to run locally, `./mvnw test` for unit tests, and `./mvnw verify` for configured verification checks. On Windows PowerShell, use `.\mvnw.cmd` instead of `./mvnw`. These commands require the future Maven scaffold and corresponding plugin configuration.

## Coding Style & Naming Conventions

Use four-space indentation in Java, lowercase package names, `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Prefer descriptive domain names and focused classes. Reuse Thymeleaf fragments for shared interface elements. No formatter or linter is configured yet; select and configure both with the application scaffold.

## Testing Guidelines

Follow `.specify/memory/constitution.md`: write failing tests before behavior changes and maintain at least 80% line and branch coverage overall and in each changed module. Cover business rules with unit tests, adapters with integration tests, and critical journeys end to end. Use `*Test` and `*IT` naming consistently with the selected runner. No testing framework is installed yet.

## Commit & Pull Request Guidelines

Use Conventional Commits, such as `chore: initialize repository` or `feat: add appointment scheduling`. There is no prior commit history to establish additional conventions. Pull requests should explain behavior changes, link relevant specifications and plans, record required approvals, and include test evidence. Include screenshots and accessibility evidence for interface changes.

## Security & Configuration

Keep credentials, patient data, and private keys out of version control and logs. Supply secrets externally and use fictitious example values. Keep wrappers, shared application configuration, and database migrations versioned; store local overrides in ignored `application-local.*` files.
