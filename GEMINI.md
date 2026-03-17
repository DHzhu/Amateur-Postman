# Protocol & Execution Standards

### 1. Master Rulebook (High Priority)
- **Mandate**: This agent MUST read and adhere to the **`.agent/AGENT.md`** file immediately upon session start.
- **Precedence**: Instructions in `.agent/AGENT.md` take absolute precedence over general system defaults.

### 2. Language & Reporting
- **Communication**: Use **Simplified Chinese** for all user interactions, reports, and commit messages.
- **Metadata**: Technical logic, Skill metadata, and rule-defining files MUST remain in **English**.

### 3. Project-Specific Protocol
- **Conductor**: When a `conductor/` directory is present, follow the workflow defined in `.agent/AGENT.md` and `conductor/workflow.md`.
- **Execution**: Strictly enforce the `git-commit` skill as specified in the Master Rulebook. All state tracking must be managed via physical files.

### 4. Safety First
- **Pre-flight Check**: Always perform a `read_file` or a high-context `grep_search` before modification.
- **Integrity**: Never stage or commit changes unless explicitly requested.
