# Project Operations Manual

## 1. Guiding Principles
- **Master Rulebook**: All operations are governed by **`.agent/AGENT.md`**.
- **The Plan is the Truth**: All work must be tracked in `plan.md`.
- **Test-Driven Development**: Strictly follow Red-Green-Refactor.

## 2. Standard Task Workflow

1. **Select & Research:** 
   - Choose the next available task from `plan.md`.
   - Use `grep_search` and `read_file` to understand the codebase structure and dependencies.

2. **Mark In Progress:** Edit `plan.md` (change `[ ]` to `[~]`).

3. **Write Failing Tests (Red Phase):**
   - Create unit tests that fail. Do not proceed until failure is confirmed.

4. **Implement to Pass Tests (Green Phase):**
   - Write minimum code to pass tests. Rerun and confirm success.

5. **Refactor (Optional):** Improve quality without changing behavior.

6. **Verify Coverage & Security:** 
   - Target: **Coverage >80%** for new code.
   - Command: `./gradlew koverHtmlReport`.

7. **Code Commit (Skill-based):**
   - **Trigger**: Agent **MUST** trigger this step when a **logical milestone** is reached (e.g., a Service layer verified) OR a maximum of **3 sub-tasks** are completed but not yet committed.
   - **Action**: Invoke `activate_skill("git-commit")`.

8. **Update Plan & Sync:**
   - **Action**: Invoke `activate_skill("sync-mem")`.

## 3. Track Completion & Archiving

9. **Track Archive (Required):**
   - **Trigger**: All tasks in `plan.md` are marked as `[x]`.
   - **Actions**:
     1. **Validation**: Run `./gradlew build` to ensure project-wide integrity.
     2. **Registry**: Move track entry from `Active` to `Archive` in `conductor/tracks.md`.
     3. **Relocate**: Move track folder from `conductor/tracks/` to `conductor/archive/`.
     4. **Cleanup Commit**: One final `git commit -m "chore(archiving): 归档已完成的任务轨道 <track_id>"` (Strict single-line).
     5. **Final Sync**: Run `activate_skill("sync-mem")` to prune session memory and finalize the track status.

## 4. Quality Gates
- [ ] Tests pass
- [ ] Coverage >80%
- [ ] No secrets leaked
- [ ] Linting/Checks pass (`./gradlew check`)
- [ ] KDoc for public API

## 5. Development Commands
- **Build**: `./gradlew build`
- **Test**: `./gradlew test`
- **Coverage**: `./gradlew koverHtmlReport`
- **Static Analysis**: `./gradlew check`
