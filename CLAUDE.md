# Project Strategy & Coordination

## 1. Bootstrap
- **Initialization**: On session start, MUST check for `conductor/index.md`.
- **Source of Truth**: If `conductor/` exists, `conductor/workflow.md` is the absolute mandate for all operations.
- **Protocol**: Follow the "Universal File Resolution Protocol" via `index.md`.

## 2. Governance
- **Tier A (Lifecycle)**: For track creation, status changes, or archiving, **MUST** `activate_skill("sync-mem")`.
- **Tier B (Tasks)**: For coding and bug fixes, follow the `Dev Cycle` in `workflow.md`.
- **Commits**: For all code changes, **MUST** `activate_skill("git-commit")`.

## 3. Technical Standards
- **Language**: 
  - User interactions, reports, commit messages, and git notes MUST be in **Simplified Chinese**.
  - All rule-defining files (`GEMINI.md`, `CLAUDE.md`, `SKILL.md`) and technical metadata MUST be in **English**.
- **Execution**: Always run physical verification (e.g., `./gradlew check`) before marking tasks complete.
- **Safety**: Perform a `read_file` "pre-flight" check before any `replace` or `write_file` operation.

## 4. Environment
- **Style**: Follow `conductor/code_styleguides/` and `Style:[ProjectName]` entity properties.
- **Tooling**: Prefer non-interactive commands. Do not invoke MCP tools directly via shell.
