---
event: BeforeTool
matcher: run_shell_command
arguments:
  command: "/git\\s+commit/"
action: skill
skill: git-commit
---

# Git Commit Skill Trigger

This hook automatically triggers the `git-commit` skill when a `git commit` command is detected.
This ensures that all commits follow the project's Chinese commit message standards and undergo validation.
