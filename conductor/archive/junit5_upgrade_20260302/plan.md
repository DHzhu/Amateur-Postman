# Implementation Plan: JUnit 5 升级

## 阶段 1: 双引擎配置引入与基础兼容性检查
- [x] **步骤 1.1**: 更新 `gradle/libs.versions.toml`。 (Completed 2026-03-03)
  - 保留现有的 `junit` 定义。
  - 新增 `junit-jupiter` (版本 `5.10.2`) 和 `junit-vintage-engine`。
  - 在 `[libraries]` 中添加对应的声明。
- [x] **步骤 1.2**: 更新 `build.gradle.kts`。 (Completed 2026-03-03)
  - 在 `dependencies` 块中加入 `testImplementation(libs.junit.jupiter)` 和 `testRuntimeOnly(libs.junit.vintage.engine)`。
  - 在 `tasks.withType<Test>() { useJUnitPlatform() }` 或者对应的测试配置代码块中启用 JUnit Platform。
- [x] **步骤 1.3**: 运行基准测试。 (Completed 2026-03-03)
  - 执行 `run_shell_command("gradlew test")`。
  - **期望结果**: 测试应完全通过（因为所有 JUnit 4 的测试将由 vintage-engine 执行）。
  - **实际结果**: ✅ 所有测试通过，双引擎配置正常工作。

## 阶段 2: 核心依赖组件”排雷”迁移
- [x] **步骤 2.1**: 迁移一个轻量级模型测试 `EnvironmentModelsTest.kt`。 (Completed 2026-03-03)
  - 将 `org.junit.Test` 和 `org.junit.Before` 替换为 `org.junit.jupiter.api.Test` 和 `org.junit.jupiter.api.BeforeEach`。
  - 将 `org.junit.Assert.*` 替换为 `org.junit.jupiter.api.Assertions.*`。
  - 运行单独的测试以验证基础 JUnit 5 注解。
- [x] **步骤 2.2**: 迁移一个重度 Mock 测试 `ScriptExecutionServiceEnhancedTest.kt`。 (Completed 2026-03-03)
  - 同样替换核心注解。
  - 执行测试。**这是最容易暴露出之前”隐藏问题”的环节**。
  - **发现问题**: JUnit 5 的 `assertTrue(message, condition)` 参数顺序与 JUnit 4 相反，需改为 `assertTrue(condition, message)`。
  - **已修复**: 修正了断言参数顺序。

## 阶段 3: 全局代码替换
- [x] **步骤 3.1**: 批量替换剩余的 17 个测试文件中的注解和导包。 (Completed 2026-03-03)
  - 包括 `@Test`, `@Before` (到 `@BeforeEach`), `@After` (到 `@AfterEach`), 以及 `Assert`。
- [x] **步骤 3.2**: 修正断言参数。 (Completed 2026-03-03)
  - JUnit 5 参数顺序变化: `assertEquals(expected, actual, message)` 而非 JUnit 4 的 `assertEquals(message, expected, actual)`。
  - 同样适用于 `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull` 等。
  - `@Test(expected = ...)` 改为 `assertThrows<Exception> { }`。
- [x] **步骤 3.3**: 运行完整测试。 (Completed 2026-03-03)
  - 确认所有 266 个测试在 Jupiter 引擎下通过。

## 阶段 4: 清理与最终验证
- [x] **步骤 4.1**: 移除旧依赖。 (Completed 2026-03-03)
  - 在 `libs.versions.toml` 中删除 `junit` 和 `junit-vintage-engine`。
  - 在 `build.gradle.kts` 中删除对这些旧库的依赖。
- [x] **步骤 4.2**: 最终构建验证。 (Completed 2026-03-03)
  - 执行 `gradlew clean build check`。
  - **结果**: ✅ 构建成功，所有 266 个测试通过。
- [x] **步骤 4.3**: 若无任何阻碍，提交本次升级的代码变更。 (Ready for commit)

---

## 升级总结

### 迁移内容
- **19 个测试文件** 从 JUnit 4 迁移到 JUnit 5
- **依赖更新**: JUnit 4.13.2 → JUnit Jupiter 5.10.2
- **移除**: `junit-vintage-engine` (不再需要向后兼容)

### 关键变更
1. **注解映射**:
   - `@Test` → `@Test` (org.junit.jupiter.api)
   - `@Before` → `@BeforeEach`
   - `@After` → `@AfterEach`

2. **断言参数顺序变化**:
   - JUnit 4: `assertEquals(message, expected, actual)`
   - JUnit 5: `assertEquals(expected, actual, message)`

3. **异常测试**:
   - `@Test(expected = Exception::class)` → `assertThrows<Exception> { }`

4. **新增依赖**:
   - `junit-platform-launcher` (IntelliJ Platform Gradle Plugin 需要)
