# Kotlin 代码风格指南 - Amateur-Postman

## 基本规则
- 遵循官方 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)。
- 简单函数优先使用表达式体。
- 参数和实参使用尾逗号。

## 命名
- **类/对象**: PascalCase（如 `PostmanToolWindowPanel`）
- **函数/属性**: camelCase（如 `sendRequest`）
- **常量**: SCREAMING_SNAKE_CASE（如 `DEFAULT_TIMEOUT`）

## 空安全
- 尽可能避免 `!!`，优先使用安全调用（`?.`）或 Elvis 运算符（`?:`）。
- `lateinit` 仅用于在 `onInit` 等生命周期方法中初始化的属性（如 UI 组件）。

## 函数式编程
- 合理使用 `let`、`run`、`apply`、`also`、`with` 以提升可读性。
- 优先不可变性（`val` 优于 `var`，`List` 优于 `MutableList`）。

## API 合规性（强制）

### 禁止使用内部 API
- **不得使用** `com.intellij.openapi.diagnostic` 以外的 `com.intellij.*` 包中标记为 `@Internal` 或 `@ApiStatus.Internal` 的类/方法。
- **不得使用** `com.sun.*`、`sun.*`、`jdk.internal.*` 等 JDK 内部 API。
- 替代方案：查阅 JetBrains 官方文档中的公共 API，或使用标准 JDK API。

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

## IntelliJ 集成
- 遵循 IDE 内置的格式化设置（定义在 `.idea/codeStyles` 中）。
- 非平凡的公共 API 使用 KDoc 文档注释。
