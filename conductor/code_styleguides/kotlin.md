# Kotlin Code Style Guide - Amateur-Postman

## General Rules
- Follow official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Prefer expression bodies for simple functions.
- Use trailing commas for parameters and arguments.

## Naming
- **Classes/Objects**: PascalCase (e.g., `PostmanToolWindowPanel`)
- **Functions/Properties**: camelCase (e.g., `sendRequest`)
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`)

## Null Safety
- Avoid `!!` whenever possible. Use safe calls (`?.`) or Elvis operator (`?:`).
- Use `lateinit` only for properties initialized in `onInit` or similar lifecycle methods (like UI components).

## Functional Programming
- Use `let`, `run`, `apply`, `also`, and `with` appropriately to improve readability.
- Prefer immutability (`val` over `var`, `List` over `MutableList`).

## IntelliJ Integration
- Respect the IDE's built-in formatting settings (defined in `.idea/codeStyles`).
- Use KDoc for documenting non-trivial public APIs.
