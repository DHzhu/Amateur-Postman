# Specification: JUnit 5 升级与兼容性验证

## 1. 背景与目标
目前代码库的测试实现使用的是 JUnit 4 (4.13.2)，但按照 `conductor/tech-stack.md` 中定义的架构和标准，测试框架应当为 JUnit 5。考虑到历史记录中曾经有升级失败的情况，本次升级的主要目标是**平滑地过渡到 JUnit 5，不破坏现有的任何测试覆盖率，并提前发现并解决所有与 IntelliJ Platform 插件相关的隐藏测试集成问题。**

## 2. 核心挑战与隐藏风险分析
根据此前的代码分析，历史升级失败可能是由以下一个或多个因素导致的：
1. **IntelliJ 平台测试框架 (`TestFrameworkType.Platform`) 隔离性问题：** 如果强行完全抛弃 JUnit 4，而平台部分组件仍依赖旧引擎或需要特定的 Test Extension，可能会导致启动即崩溃。
2. **Gradle 测试引擎配置不当：** 遗漏配置 `tasks.test { useJUnitPlatform() }`，导致 Gradle 继续用旧引擎跑测试而无法发现 JUnit 5 测试用例。
3. **Mockito 冲突或并发异常：** 项目中存在大量 `mockito-kotlin`，尤其是 `mock<Project>()` 之类的基础组件级 mock。如果直接去掉 JUnit 4 可能会导致 mock 对象生命周期管理出现竞态条件。
4. **API 参数位置变化：** JUnit 5 中 `assertEquals` 等断言参数的顺序变化（特别是 message 参数），如果不做替换可能静默失败。

## 3. 技术方案与实施策略
**方案概述：双引擎过渡机制 (Vintage Engine)**

1. **第一阶段：环境准备与隔离测试（双引擎并行）**
   * 修改 `gradle/libs.versions.toml`，引入 `junit-jupiter` 及其 `junit-vintage-engine`。
   * 修改 `build.gradle.kts`，在 Gradle 的 `test` 任务中启用 `useJUnitPlatform()`。
   * **验证**：不修改任何现有 `.kt` 测试代码，直接运行 `./gradlew test`，观察 Vintage 引擎是否能完美兼容现有的 JUnit 4 测试。
2. **第二阶段：局部迁移与 Mockito 兼容性验证（“排雷”测试）**
   * 挑选一个简单的无依赖模型测试（如 `EnvironmentModelsTest.kt`）和一个强依赖 Mockito 的服务测试（如 `ScriptExecutionServiceEnhancedTest.kt`）迁移至 JUnit 5。
   * 验证在 JUnit Jupiter 引擎下，IntelliJ Context（如 Project 实例）的 Mocking 是否会导致测试死锁或报错。
3. **第三阶段：全面代码迁移**
   * 批量替换所有测试代码的导入，包括 `@Test`, `@BeforeEach`, 和 `org.junit.jupiter.api.Assertions.*`。
   * 处理具体的 Assertion 语法调整。
4. **第四阶段：移除旧引擎与清理**
   * 从 Gradle 配置中彻底移除 `junit 4` 及其 `vintage-engine`，只保留 `junit-jupiter`。
   * 运行完整的项目编译和平台插件验证测试 (`./gradlew buildPlugin` 和 `./gradlew verifyPlugin`) 以确保 CI 环境的兼容性。
   * 同步更新相关文档。
