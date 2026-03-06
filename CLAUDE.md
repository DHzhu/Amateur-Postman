# Project Memory & Coordination Protocol

## 1. Governance & Tiered Protocols

- **Zero-Trust Pre-flight (Session Init)**: NEVER blindly trust current session snapshots.
  - **Action**: Empirically verify physical reality via directory listing and precise memory access tools for `Project:[ProjectName]`.
- **Tier A: Lifecycle Changes** (Track Creation, Completion, Archiving):
  - **Protocol**: **MUST** `activate_skill("sync-mem")` and follow its expert procedural guidance.
- **Tier B: Functional Tasks** (Coding, Docs, Bug Fixes):
  - **Memory Interaction**: When accessing a track via its memory pointer:
    1. Resolve the `Track:[ProjectName]:[ID]` entity's `file_path`.
    2. **MUST** read both `spec.md` (requirements) and `plan.md` (execution logs) in the same directory.
    3. **Surgical Read-Before-Write**: Always `read_file` the target immediately before any `replace` or `write_file` to prevent stale context.
  - **Audit**: Concise one-line confirmation of the action.

## 2. Technical Standards

- **Primary Interaction**: Use **Simplified Chinese (简体中文)** for all user-facing responses and reports.
- **Rules & Metadata**: **English** is mandatory for all rule-defining files (`GEMINI.md`, `CLAUDE.md`, `SKILL.md`), technical metadata, and tool configuration.
- **Naming Policy**: All memory entities MUST use project-prefixed names (e.g., `Track:[ProjectName]:[ID]`).
- **Path Rule**: All `file_path` entries MUST be absolute paths.

## 3. Development Standards

- **Coding Style**: Follow the `Style:[ProjectName]` entity properties and `conductor/code_styleguides/`.
- **Tool Isolation**: MCP tools are internal; **NEVER** attempt to invoke them through `run_shell_command`.
- **Environment**: All paths must be absolute within the current OS environment.
