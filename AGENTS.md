# Repository Guidelines

## Project Structure & Module Organization

This repository holds course materials, requirements, and the shared WEMOVE application for Software Development Practice 2. Authored implementation work is collected under `project-implementation/`; its [README](project-implementation/README.md) is the engineering entry point.

- `course-materials/`: course instructions, website reconstruction requirements, and `课程分工.md`, which lists individual and group deliverables and grading weights.
- `project-implementation/project-requirements/`: authored requirements, collaboration rules, shared interface agreements, and acceptance traceability. Keep supplied reference documents in its `references/` subdirectory.
- `project-implementation/apps/web/`: the shared Vue 3 + TypeScript application. Source is in `src/`, frontend unit tests are colocated `*.spec.ts`, and runtime static assets are in `public/`.
- `project-implementation/apps/api/`: the shared Java 21 + Spring Boot application. Source is in `src/main/java/`, backend tests are in `src/test/java/`, and Flyway migrations are in `src/main/resources/db/migration/`. Existing Java packages remain under `com.wemove.identity`, including the `catalog` business package.
- `project-implementation/contracts/openapi/`: module interface contracts, currently `identity.yaml` and `catalog.yaml`.
- `project-implementation/scripts/smoke/`: API smoke scripts, currently `identity-api.sh` and `catalog-api.ps1`.
- `project-implementation/docs/modules/`: module design, runbooks, member contribution records, and verification reports organized by business domain (`identity/`, `catalog/`).
- `project-implementation/docs/design/references/`: design reference images; these are separate from application runtime assets.
- `project-implementation/docs/verification/`: verification reports and visual evidence, with module-specific evidence in domain subdirectories.
- `project-implementation/tests/cases/`: member and cross-module acceptance test designs. Review status and execution evidence are separate from unit-test results.
- `project-implementation/.env.example`: shared environment template. `.env` and `.local/` hold local configuration and artifacts; `.superdesign/` holds design-tool metadata.
- Existing `project-implementation/ideation/`, `project_flowchart_view/`, and `前端设计视觉参考/` retain their current purpose and location.

Organize files by engineering responsibility and business domain, never by member labels such as `partA/` or `partB/`. Maintain member ownership in the collaboration and acceptance documents. Extend the shared applications when adding business modules. Keep unit tests beside their corresponding application code and keep database migrations in the backend's Flyway discovery path. Preserve course-provided originals in their existing locations.

## Build, Test, and Development Commands

Prerequisites are JDK 21, Maven 3.9+, Node.js 20+, npm 10+, and MySQL 8.0+. Database setup and environment configuration are documented in the [identity runbook](project-implementation/docs/modules/identity/运行与验证手册.md); the [catalog runbook](project-implementation/docs/modules/catalog/运行与验证手册.md) also provides PowerShell and catalog smoke-test instructions.

Run the following commands from the repository root:

- `npm --prefix project-implementation/apps/web ci`: install frontend dependencies from the lockfile.
- `npm --prefix project-implementation/apps/web run dev`: start the frontend development server; `/api` is proxied to port 8080.
- `mvn -f project-implementation/apps/api/pom.xml spring-boot:run`: start the backend after configuring MySQL and exporting the variables from `project-implementation/.env` as described in the identity runbook.
- `mvn -f project-implementation/apps/api/pom.xml verify`: run backend tests and Maven verification.
- `npm --prefix project-implementation/apps/web run test`: run frontend Vitest tests.
- `npm --prefix project-implementation/apps/web run build`: run Vue TypeScript checking and produce the Vite build.

Use these repository checks when preparing changes:

- `git status --short`: inspect modified and untracked files.
- `git diff --check`: detect whitespace errors in text changes.
- `git diff --stat`: review the scope of a contribution.

Keep commands aligned with `project-implementation/apps/web/package.json`, `project-implementation/apps/api/pom.xml`, and both runbooks. Report which checks were actually executed; the presence of a command or a prior verification record does not establish that a new change passed it. Build outputs belong in `apps/web/dist/` and `apps/api/target/` and must remain ignored along with local dependencies and temporary artifacts.

## Documentation Style & Naming Conventions

Use descriptive Chinese filenames consistent with the existing materials. Prefer Markdown for editable notes and discussion summaries; preserve PDF and DOCX formats for supplied originals. Use clear headings, hyphenated lists, and readable Markdown tables. Follow the existing four-space indentation for nested lists.

Record discussion decisions and outcomes rather than copying chat logs, as required by `course-materials/课程分工.md`. No language-specific formatter or linter is configured.

## Validation & Testing Guidelines

Preview changed Markdown and check relative links and tables. Open modified PDF or DOCX files in a suitable viewer to verify readability and layout. Frontend tests use Vitest and Vue Test Utils; backend tests use the Spring Boot test dependencies and Maven Surefire. No coverage threshold or CI workflow is configured. Run checks appropriate to the change and provide reproducible test cases and execution evidence for the course test report.

Keep unexecuted test designs marked as such. The 159 member and integration cases in `tests/cases/` await review; the existing 4 frontend and 9 backend unit tests are a separate set of checks. Passing those unit tests does not establish that the acceptance cases were executed or passed. Both API smoke scripts require a test database and can create or modify test data; follow their runbooks against the intended database. The identity script requires Linux or WSL with Bash, curl, and jq, plus a built backend JAR and a free application port; it starts and stops its own backend. The catalog script requires PowerShell and an already running test application.

## Commit & Pull Request Guidelines

Existing commits use short English descriptions such as `Add course materials and project requirement docs`; no enforced commit format is evident. Keep commits focused and use concise, action-oriented messages.

For pull requests, describe the changed deliverables, motivation, and validation performed. Link relevant requirements or issues when available; include screenshots for visual changes. Exclude local metadata such as `.DS_Store`, which is already ignored.
