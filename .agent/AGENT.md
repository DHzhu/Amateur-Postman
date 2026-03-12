# Agent Master Protocol (AMP)

## 1. Identity & Context
- **Bootstrap**: Agent **MUST** locate `conductor/index.md` on session start. Use "Universal File Resolution Protocol" to resolve project docs.
- **Precedence**: `AGENT.md` defines **HOW** (Principles); `index.md` defines **WHAT** (Context/CLI).

## 2. Core Mandates
- **Security**: **NEVER** log/commit secrets. Protect config/system folders.
- **Language**: Agent **MUST** use **Chinese** for user interactions/audit notes and **English** for technical rules/metadata.
- **Integrity**: All progress **MUST** be tracked in `plan.md`. Agent **MUST STOP** and wait for Directive after Track initialization.

## 3. Governance & Workflow
- **Lifecycle**: Agent **MUST** use `sync-mem` for state sync. Follow `workflow.md` for tasks.
- **Prep**: Perform "pre-flight" check (read_file/grep_search) before any modifications.
- **Commits**: Code changes **MUST** use `activate_skill("git-commit")`. Format **MUST** be: `<type>(<scope>): <Chinese description>`. Body text, `Co-Authored-By`, or feature lists are **STRICTLY FORBIDDEN**.

## 4. Quality Gates (DoD)
A task is **DONE** ONLY when:
1. **Standards**: **MUST** pass project verification, TDD verified, and Coverage >80%.
2. **Audit**: **MUST** be committed with Git Notes and SHA synchronized in `plan.md`.
3. **Graph**: Knowledge Graph **MUST** be synchronized via `sync-mem` to reflect track/task status.
