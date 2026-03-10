# Agent Master Protocol (AMP) - Version 2.0 (Universal)

## 1. Bootstrap & Identity
- **Immediate Action**: On session start, the Agent **MUST** first locate and read **`conductor/index.md`**.
- **Context Discovery**: Use the "Universal File Resolution Protocol" (defined in `index.md`) to resolve project-specific documents.
- **Precedence**: This `AGENT.md` defines the **HOW** (Global Principles), while the files resolved via `index.md` define the **WHAT** (Project Context) and **CLI** (Concrete Commands).

## 2. Core Mandates
- **Security**: NEVER log/print/commit secrets. Protect sensitive system/config folders.
- **Language**: **Simplified Chinese** for user interactions, reports, and audit notes. **English** for technical metadata and rules.
- **Workflow Integrity**: All progress MUST be tracked in `plan.md` via the Conductor tracks system.

## 3. Governance (Conductor Mode)
- **Lifecycle (Tier A)**: Track/Status changes **MUST** use `activate_skill("sync-mem")` for state synchronization.
- **Tasks (Tier B)**: Follow the workflow defined in the project's **`workflow.md`**.
  - **Batching**: Group 2-3 logically related sub-tasks into one `git-commit` cycle.
  - **Architecture Mapping**: Map complex features in the Knowledge Graph before coding. Use prefix `Component:`, `Service:`, or `LogicFlow:`.
- **Commits**: Code changes **MUST** use `activate_skill("git-commit")`.

## 4. Engineering Standards (Quality Gates)
A task is only eligible for completion if it satisfies these universal criteria (concrete commands are retrieved from `workflow.md`):
- **Verification**: Must pass all **Project Verification Commands** (e.g., Linting, Static Analysis, Type Checking).
- **Testing**: Must follow TDD (Red-Green-Refactor). All unit/integration tests must pass.
- **Coverage**: New code coverage must meet the **Project Coverage Target** (Default: >80%).
- **Safety**: Perform a "pre-flight" check (`read_file` or high-context `grep_search`) before any modification.

## 5. Definition of Done (DoD)
A task is DONE when:
1. It meets all criteria in Section 4.
2. It is committed with an audit note (Git Notes).
3. The `plan.md` is updated with the associated SHA.
4. The Knowledge Graph is synchronized via `sync-mem`.
