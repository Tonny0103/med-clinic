# Repository Guidelines

## Structure

Med Clinic is a Java 21 / Spring Boot 4.1.2 monolith with Thymeleaf. This is a demo MVP with dozens of fictitious patients. Java lives in src/main/java/br/com/medclinic, templates in src/main/resources/templates, CSS in static, migrations in db/migration and tests in src/test/java. Feature documents live in specs/001-gestao-pacientes; deployment scripts live in ops/.

Separate domain, application, infrastructure and presentation. Domain rules remain independent of Spring and persistence. Application contracts isolate JDBC, private files and the scanner. Keep the implementation proportional to a single clinic and a small VM.

## Build and checks

Use the committed Maven Wrapper with JDK 21: ./mvnw test, ./mvnw verify spotless:check and ./mvnw spotless:apply. In Windows PowerShell use .\mvnw.cmd. ops/Start-Local.ps1 prepares ignored local credentials and starts H2 with the optional demo seed. docker compose up -d --build runs the app, PostgreSQL and ClamAV.

Use four-space Java indentation, lowercase packages, PascalCase classes, camelCase fields and UPPER_SNAKE_CASE constants. Spotless/Palantir formats Java; the compiler uses -Xlint:all. Prefer focused classes and shared Thymeleaf fragments.

## Verification and delivery

Follow constitution 2.0.0 and the user's scope: a few essential tests for identity, permissions, preservation and transactions, written before critical behavior changes. Use *Test for the current Surefire suite. No minimum coverage, load tests, separate staging or formal accessibility certificate is required. Complement the tests with the short demo script and record only checks actually performed.

Use Conventional Commits and commit each completed task individually, as requested by the user. Keep spec, plan and tasks consistent with the implementation. No separate product/technical role approvals or document hashes are required for this demonstration.

## Security and configuration

Use fictitious patients only. Never commit credentials, real patient data, private keys, database files or private attachments; keep secrets out of logs. Configure secrets externally; .env, data/, target/, backups/ and application-local.* are ignored. Keep the Maven Wrapper, shared configuration and migrations versioned. Enforce role permissions and CSRF on the server. No role can read clinical content in this module.
