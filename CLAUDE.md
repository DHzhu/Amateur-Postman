# Project Memory & Coordination Protocol

## 1. Governance & Tiered Protocols
- **Zero-Trust Pre-flight (Session Init)**: NEVER blindly trust the logical `state_snapshot`. The very first action of any session MUST be a physical-first check (`ls -R conductor/` and `read_graph`) to empirically verify active tracks.
- **Tier A: Lifecycle Changes** (Track Creation, Completion, Archiving):
  - **Protocol**: **MUST** `activate_skill("sync-mem")` and follow its expert procedural guidance for L1-L4 synchronization.
  - **Audit**: Mandatory detailed Audit Report (Compare Graph vs Files).
- **Tier B: Functional Tasks** (Coding, Docs, Bug Fixes):
  - **Protocol**: Surgical **Read-Before-Write**. Call `read_graph` and target `read_file` to ensure safety.
  - **Audit**: Concise one-line confirmation.

## 2. Technical Standards
- **Primary Interaction**: Use **Simplified Chinese (简体中文)** for all user-facing responses and reports.
- **Rules & Metadata**: **English** is mandatory for all rule-defining files (`GEMINI.md`, `SKILL.md`), technical metadata, and MCP tool logic.
- **Path Rule**: All `file_path` entries MUST be absolute paths.

## 3. Development Standards
- **Coding Style**: Follow the `Style:[ProjectName]` entity properties and `conductor/code_styleguides/`.
- **WSL Environment**: All paths must be WSL-absolute.
