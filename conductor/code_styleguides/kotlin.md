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

## API 合规性（强制）

### 禁止使用 Internal API
- **不得使用** `com.intellij.openapi.diagnostic` 以外的 `com.intellij.*` 包中标记为 `@Internal` 或 `@ApiStatus.Internal` 的类/方法。
- **不得使用** `com.sun.*`、`sun.*`、`jdk.internal.*` 等 JDK 内部 API。
- 替代方案：查找 JetBrains 官方文档中的公共 API，或使用标准 JDK API。

### 禁止使用已废弃 API
- **不得调用** 标记为 `@Deprecated` 的方法，除非有 `ReplaceWith` 且已应用替换。
- **不得使用** Kotlin 标准库中标记为 `@Deprecated` 的函数（如 `kotlin.io.createTempFile`），应使用推荐替代。
- 第三方库（OkHttp、Jackson 等）的废弃 API 同样禁止使用。

### 注解使用站点目标
- data class 构造参数上的 Jackson 注解（`@JsonProperty` 等）**必须**使用 `@field:` 限定符，避免 Kotlin 注解目标歧义警告。
  ```kotlin
  // ✅ 正确
  data class Foo(@field:JsonProperty("bar") val bar: String)

  // ❌ 错误 — 会产生编译警告
  data class Foo(@JsonProperty("bar") val bar: String)
  ```

### 协程 API
- `GlobalScope` 属于 `@DelicateCoroutinesApi`，测试中使用时**必须**添加 `@OptIn(DelicateCoroutinesApi::class)`。
- 生产代码**禁止**使用 `GlobalScope`，应使用项目级 `CoroutineScope` 或 IntelliJ 的 `ProgressManager`。

### 验证方式
每次提交前执行 `./gradlew clean compileKotlin compileTestKotlin --no-build-cache`，确认输出中无 `^w:` 警告。

## IntelliJ Integration
- Respect the IDE's built-in formatting settings (defined in `.idea/codeStyles`).
- Use KDoc for documenting non-trivial public APIs.
