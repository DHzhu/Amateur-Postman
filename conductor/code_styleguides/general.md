# General Code Style Principles

This document outlines general coding principles that apply across all languages and frameworks used in this project.

## Readability
- Code should be easy to read and understand by humans.
- Avoid overly clever or obscure constructs.

## Consistency
- Follow existing patterns in the codebase.
- Maintain consistent formatting, naming, and structure.

## Simplicity
- Prefer simple solutions over complex ones.
- Break down complex problems into smaller, manageable parts.

## Maintainability
- Write code that is easy to modify and extend.
- Minimize dependencies and coupling.

## API 合规性（强制）
- **禁止使用 Internal API**: 不得调用框架或库中标记为 Internal 的接口。对于 IntelliJ 插件，以 Plugin Verifier 报告为准。
- **禁止使用已废弃 API**: 不得调用标记为 Deprecated 的方法，必须使用推荐替代方案。
- **编译零警告**: 代码提交前必须通过 `./gradlew clean compileKotlin compileTestKotlin --no-build-cache` 且无 `^w:` 警告。

## Documentation
- Document *why* something is done, not just *what*.
- Keep documentation up-to-date with code changes.
