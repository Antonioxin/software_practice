# Repository Guidelines

## Project Structure & Module Organization

This repository currently holds materials for Software Development Practice 2; it does not yet contain application source code, automated tests, or application assets.

- `course-materials/`: course instructions, website reconstruction requirements, and `课程分工.md`, which lists individual and group deliverables and grading weights.
- `project-implementation/project-requirements/references/`: reference material for preparing project requirements, including the CMU example PDF.

Keep course-provided originals in their existing locations. Place authored requirements under `project-implementation/project-requirements/`, separate from reference documents. When implementation begins, document the chosen source, test, and asset directories here.

## Build, Test, and Development Commands

No build system, dependency manifest, local server, or automated test command is configured. Use these repository checks when preparing changes:

- `git status --short`: inspect modified and untracked files.
- `git diff --check`: detect whitespace errors in text changes.
- `git diff --stat`: review the scope of a contribution.

Add verified installation, development, build, and test commands when introducing application code.

## Documentation Style & Naming Conventions

Use descriptive Chinese filenames consistent with the existing materials. Prefer Markdown for editable notes and discussion summaries; preserve PDF and DOCX formats for supplied originals. Use clear headings, hyphenated lists, and readable Markdown tables. Follow the existing four-space indentation for nested list items.

Record discussion decisions and outcomes rather than copying chat logs, as required by `course-materials/课程分工.md`. No language-specific formatter or linter is configured.

## Validation & Testing Guidelines

Preview changed Markdown and check relative links and tables. Open modified PDF or DOCX files in a suitable viewer to verify readability and layout. No testing framework or coverage threshold exists yet. Future implementation contributions should document their test command and provide reproducible test cases for the course test report.

## Commit & Pull Request Guidelines

Existing commits use short English descriptions such as `Add course materials and project requirement docs`; no enforced commit format is evident. Keep commits focused and use concise, action-oriented messages.

For pull requests, describe the changed deliverables, motivation, and validation performed. Link relevant requirements or issues when available; include screenshots for visual changes. Exclude local metadata such as `.DS_Store`, which is already ignored.
