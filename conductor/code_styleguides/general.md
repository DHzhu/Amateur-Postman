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

## API Compliance (Mandatory)
- **No Internal APIs**: Do not call interfaces marked as Internal by frameworks or libraries. For IntelliJ plugins, defer to the Plugin Verifier report.
- **No Deprecated APIs**: Do not call methods marked as Deprecated; use the recommended replacement.
- **Zero Compiler Warnings**: Code must pass `./gradlew clean compileKotlin compileTestKotlin --no-build-cache` with no `^w:` warnings before committing.

## Documentation
- Document *why* something is done, not just *what*.
- Keep documentation up-to-date with code changes.
