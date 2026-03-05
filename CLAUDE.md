# Project Memory & Coordination Protocol

## 1. Governance & Tiered Protocols
- **Zero-Trust Pre-flight (Session Init)**: NEVER blindly trust current session snapshots.
  - **Action**: Empirically verify physical reality via directory listing and precise memory access tools for `Project:Amateur-Postman` and `Plan:Amateur-Postman`.
- **Tier A: Lifecycle Changes** (Track Creation, Completion, Archiving):
  - **Protocol**: **MUST** `activate_skill("sync-mem")` and follow its expert procedural guidance.
- **Tier B: Functional Tasks** (Coding, Docs, Bug Fixes):
  - **Memory Interaction Rule**: When accessing a track via its memory pointer:
    1. Resolve the `Track:[ProjectName]:[ID]` entity's `file_path`.
    2. **MUST** read both `spec.md` (for requirements/constraints) and `plan.md` (for execution logs) in the same directory.
    3. Perform surgical **Read-Before-Write**.
  - **Audit**: Concise one-line confirmation of the action.

## 2. Technical Standards
- **Primary Interaction**: Use **Simplified Chinese (简体中文)** for all user-facing responses and reports.
- **Rules & Metadata**: **English** is mandatory for all rule-defining files (`GEMINI.md`, `SKILL.md`), technical metadata, and tool configuration.
- **Naming Policy**: All memory entities MUST use project-prefixed names (e.g., `Track:Amateur-Postman:[ID]`).
- **Path Rule**: All `file_path` entries MUST be absolute paths.

## 3. Development Standards
- **Coding Style**: Follow the `Style:Amateur-Postman` entity properties and `conductor/code_styleguides/`.
- **Environment**: All paths must be absolute within the current OS environment.
