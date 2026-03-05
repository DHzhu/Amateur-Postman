# Specification: Mock Server & Test Infrastructure Hotfix

## 背景 (Context)
项目在试图引入 JUnit 5 的过程中，由于部分测试类仍继承自 JUnit 4 遗留基类 `BasePlatformTestCase`，导致在 JUnit 5 运行环境下出现 `lateinit` 初始化失败、生命周期不匹配等系统性回归。

## 目标 (Goals)
1. **基线固化**: 明确当前失败测试的边界，建立可重复的阶段性回归验证机制。
2. **全面原生化**: 彻底废弃对 `BasePlatformTestCase` (JUnit 4) 的继承。
3. **JUnit 5 架构**: 使用 IntelliJ SDK 2025.1 推荐的 `TestFixture` 组合模式或 JUnit 5 扩展模型重写测试基座。
4. **消除回归**: 修复 Mock Server 生命周期 Bug 和 Body 读取逻辑，确保 120+ 测试在纯 JUnit 5 环境下通过。

## 关键技术点 (Technical Details)
- **阶段性验证**: 采用“修复一组 -> 验证一组 -> 全量回归”的闭环策略。
- **JUnit 5 组合模式**: 采用 `IdeaTestFixtureFactory` 在 `@BeforeEach` 中手动创建 `CodeInsightTestFixture`。
- **生命周期对齐**: 使用 `@BeforeEach` 和 `@AfterEach` 精确控制 IntelliJ 测试环境的启动与销毁。
- **Mock Server 重构**: 将 `executor` 移至 `start()`，实现真正的热重启支持。
- **流式 Body 解析**: 移除对 `Content-Length` 的依赖，采用循环流式读取。
