---
name: require-git-commit-skill
enabled: true
event: bash
action: skill
skill: git-commit
pattern: git\s+commit
---

**Notice: git commit detected. Automatically triggering git-commit skill.**

**The skill provides:**
- ✅ Chinese single-line commit message validation (Conventional Commit format)
- ✅ Git Notes audit (attach task summary to commit)
- ✅ Plan sync (update plan.md task status)
- ✅ Knowledge graph sync (auto-trigger sync-mem)

This ensures all commits comply with the project's Git workflow standards.
