# Project Operations Manual

## 1. Guiding Principles
- **Master Rulebook**: All operations are governed by **`.agent/AGENT.md`**.
- **The Plan is the Truth**: All work must be tracked in `plan.md`.
- **Test-Driven Development**: Strictly follow Red-Green-Refactor.

## 2. Standard Task Workflow

1. **Select & Research:** 
   - Choose the next available task from `plan.md`.
   - Use `grep_search` and `read_file` to understand the codebase structure and dependencies.

1.5 **Research Sync & Plan Alignment (Critical):**
   - **Direct Sync**: Findings from any **Research Phase** MUST be directly integrated into **`spec.md`** (updating Technical Strategy or Constraints) and **`plan.md`** (refining or adding sub-tasks).
   - **No Extra Files**: Do NOT create separate `research.md` files unless explicitly requested. The `spec.md` and `plan.md` are the living documentation.
   - **Confirmation**: For major architectural shifts discovered during research, the Agent MUST stop and inform the user before proceeding to implementation.

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

8. **Finalize Task in Plan:**
   - **Action**: Ensure `plan.md` reflects the completion of the task with the correct SHA. No graph synchronization is required.

## 3. Track Completion & Archiving

9. **Track Archive (Required):**
   - **Trigger**: All tasks in `plan.md` are marked as `[x]`.
   - **Actions**:
     1. **Validation**: Run `./gradlew build` to ensure project-wide integrity.
     2. **Documentation**: Update **`CHANGELOG.md`** with the significant changes from this track, following the [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format.
     3. **Metadata Update**: Update `metadata.json` in the track folder:
        - Set `"status": "done"`.
        - Ensure fields follow the **Standard Metadata Template** (see below).
        - Update `id` to include the date suffix (e.g., `track_id_YYYYMMDD`).
     4. **Registry**: Move track entry from `Active` to `Archive` in `conductor/tracks.md`.
     5. **Relocate**: Move track folder from `conductor/tracks/` to `conductor/archive/`.
     6. **Cleanup Commit**: One final `git commit -m "chore(archiving): 归档已完成的任务轨道 <track_id>"` (Strict single-line).

### Standard Metadata Template
```json
{
  "id": "track_id_YYYYMMDD",
  "title": "Human-readable Title",
  "description": "Short description of the track goal.",
  "status": "done",
  "created_at": "YYYY-MM-DD",
  "priority": "high|medium|low",
  "tags": ["feature", "refactor", "etc"]
}
```

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
