# Project Operations Manual

## 1. Guiding Principles
- **Master Rulebook**: All operations are governed by **`.agent/AGENT.md`**.
- **The Plan is the Truth**: All work must be tracked in `plan.md`.
- **Test-Driven Development**: Strictly follow Red-Green-Refactor.

## 2. Standard Task Workflow

1. **Select & Probe Task:** 
   - Choose the next available task from `plan.md`.
   - **Architectural Mapping:** For complex features, manually map the impacted architecture in the Knowledge Graph before coding (see `.agent/AGENT.md` section 2). Use prefix `Component:`, `Service:`, or `LogicFlow:`.

2. **Mark In Progress:** Edit `plan.md` (change `[ ]` to `[~]`).

3. **Write Failing Tests (Red Phase):**
   - Create unit tests that fail. Do not proceed until failure is confirmed.

4. **Implement to Pass Tests (Green Phase):**
   - Write minimum code to pass tests. Rerun and confirm success.

5. **Refactor (Optional):** Improve quality without changing behavior.

6. **Verify Coverage & Security:** 
   - Target: **>80% coverage** for new code.
   - Run: `./gradlew koverHtmlReport`.

7. **Code Commit (Skill-based):**
   - **Batching**: Support grouping 2-3 logical sub-tasks.
   - **Action**: Invoke `activate_skill("git-commit")`.

8. **Update Plan & Sync:**
   - **Action**: Invoke `activate_skill("sync-mem")`.

## 3. Checkpointing Protocol
Follow Tiered Verification (Milestone vs. Iterative) as defined in `.agent/AGENT.md`.

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
