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
- ✅ Single-line Chinese commit message validation
- ✅ Commit format check (via commit-check.sh)
- ✅ Automatic retry mechanism
- ✅ Automatic push prompt

This ensures all commits comply with the project's Chinese commit message standards.
