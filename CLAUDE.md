# Coordination Protocol

## 1. Master Rulebook
- **Source of Truth**: This agent MUST read and strictly adhere to **`.agent/AGENT.md`** for all operations.
- **Bootstrapping**: Upon session start, follow the "Universal File Resolution Protocol" through `conductor/index.md`.

## 2. Governance
- **Lifecycle**: For all track-level changes, invoke `activate_skill("sync-mem")`.
- **Tasks**: Follow the workflow defined in `.agent/AGENT.md` and `conductor/workflow.md`.
- **Commits**: For code changes, invoke `activate_skill("git-commit")`.

## 3. Standards
- **Language**: Simplified Chinese for user interactions, reports, and Git notes.
- **Quality**: Enforce >80% coverage and no security leaks.
- **Safety**: High-context `grep_search` is acceptable for "pre-flight" checks.
