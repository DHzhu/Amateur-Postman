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

## API Compliance (Mandatory)

### No Internal APIs
- Do **not** use classes/methods marked `@Internal` or `@ApiStatus.Internal` in `com.intellij.*` packages, except `com.intellij.openapi.diagnostic`.
- Do **not** use JDK internal APIs: `com.sun.*`, `sun.*`, `jdk.internal.*`.
- Alternative: use public APIs from JetBrains documentation, or standard JDK APIs.

### No Deprecated APIs
- Do **not** call methods marked `@Deprecated`, unless a `ReplaceWith` is provided and applied.
- Do **not** use deprecated Kotlin stdlib functions (e.g., `kotlin.io.createTempFile`); use the recommended replacement.
- Deprecated APIs in third-party libraries (OkHttp, Jackson, etc.) are equally prohibited.

### Annotation Use-Site Targets
- Jackson annotations (`@JsonProperty`, etc.) on data class constructor parameters **must** use `@field:` qualifier to avoid Kotlin annotation target ambiguity warnings.
  ```kotlin
  // ✅ Correct
  data class Foo(@field:JsonProperty("bar") val bar: String)

  // ❌ Wrong — produces compiler warning
  data class Foo(@JsonProperty("bar") val bar: String)
  ```

### Coroutine APIs
- `GlobalScope` is `@DelicateCoroutinesApi`; tests using it **must** add `@OptIn(DelicateCoroutinesApi::class)`.
- Production code **must not** use `GlobalScope`; use a project-level `CoroutineScope` or IntelliJ's `ProgressManager`.

### Verification
Before every commit, run `./gradlew clean compileKotlin compileTestKotlin --no-build-cache` and confirm no `^w:` warnings in the output.

## IntelliJ Integration
- Respect the IDE's built-in formatting settings (defined in `.idea/codeStyles`).
- Use KDoc for documenting non-trivial public APIs.
