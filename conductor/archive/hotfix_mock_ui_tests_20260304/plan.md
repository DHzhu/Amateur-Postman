# Implementation Plan: Mock Server & Test Infrastructure Hotfix

## 执行策略（风险优先）
- 先修复会导致测试体系不可用/功能错误的阻断问题（测试基座、脚本回归、Mock 生命周期）。
- 再处理鲁棒性与性能细节（流式读取、UI 内存拷贝与状态机）。
- 每个阶段结束必须跑对应最小回归集，避免"改完再一起炸"。

> 预计总工时：8h - 12h（含排查与回归时间，不含人工验收等待）

## 阶段 0: 基线确认与回归分组（0.5h）
- [x] **步骤 0.1**: 固化失败基线：记录当前失败测试列表（ScriptExecutionServiceEnhancedTest / VariableSourceTest / QuickLookPanelTest）。
- [x] **步骤 0.2**: 建立三组回归命令：
	- A组（测试基建）：VariableSourceTest, QuickLookPanelTest, CollectionsPanelPerformanceTest
	- B组（脚本引擎）：ScriptExecutionServiceEnhancedTest
	- C组（Mock）：MockServerMethodTest, MockServerOomTest, MockServerPriorityTest

**完成标准（DoD）**
- [x] 回归命令可重复执行，输出可追溯。

## 阶段 1: JUnit 5 测试基建修复（1.5h - 2.5h）
- [x] **步骤 1.1**: 移除测试中对 `BasePlatformTestCase` 的继承，改为纯 JUnit 5 风格测试类。
- [x] **步骤 1.2**: 对确需 IntelliJ fixture 的测试采用组合式初始化；不需要 fixture 的测试保持最小依赖（避免过度引入）。
- [x] **步骤 1.3**: 修复 `VariableSourceTest.kt` 与 `QuickLookPanelTest.kt` 的 `setUp/tearDown` 生命周期，消除 `lateinit` 崩溃。
- [x] **步骤 1.4**: 验证 `CollectionsPanelPerformanceTest.kt` 在新基座下稳定运行。

**阶段回归命令**
- [x] `./gradlew test --tests "*VariableSourceTest" --tests "*QuickLookPanelTest" --tests "*CollectionsPanelPerformanceTest"`

**完成标准（DoD）**
- [x] A组测试全绿。（VariableSourceTest: 4/4, QuickLookPanelTest: 4/4, CollectionsPanelPerformanceTest: ✅）
- [x] 无新增 JUnit4 风格生命周期调用。

## 阶段 2: 脚本引擎回归修复（2h - 3h）
- [x] **步骤 2.1**: 定位 `ScriptExecutionServiceEnhancedTest.kt` 失败根因（区分：库注入问题 vs pm 包装语义问题）。
  - 根因：GraalVM 跨 ScriptContext 对象迁移（OtherContextGuestObject.migrateReturn）触发 AssertionError
- [x] **步骤 2.2**: 修复 `chai/ajv` 注入与 `pm.response` 包装兼容性，确保断言链语义一致。
  - 修复：改为 ENGINE_SCOPE 注入；移除 chaiRef/ajvRef Java 引用；preamble 执行 JS 回调后仅传 Boolean
- [x] **步骤 2.3**: 修复 `pm.sendRequest` 链路（请求解析、回调结果对象、错误分支）并补充最小回归用例。

**阶段回归命令**
- [x] `./gradlew test --tests "*ScriptExecutionServiceEnhancedTest"`

**完成标准（DoD）**
- [x] B组测试全绿。（ScriptExecutionServiceEnhancedTest: 38/38, ScriptExecutionServiceTest: 8/8）
- [x] `pm.sendRequest` 成功/失败路径均有测试覆盖。

**关键提交**: `29b5ceb` fix(script): resolve GraalVM cross-ScriptContext interop primitive error

## 阶段 3: Mock Server 生命周期与Body读取修复（1.5h - 2.5h）
- [x] **步骤 3.1**: 调整 `MockServerManager.kt` executor 生命周期，保证 stop 后可再次 start。
- [x] **步骤 3.2**: 将请求体读取改为循环流式读取，不依赖 `Content-Length`，并保留上限保护。
- [x] **步骤 3.3**: 补充/修正重启与分块传输场景测试。

**阶段回归命令**
- [x] `./gradlew test --tests "*MockServerMethodTest" --tests "*MockServerOomTest" --tests "*MockServerPriorityTest"`

**完成标准（DoD）**
- [x] C组测试全绿。（MockServerMethodTest: 8/8, MockServerOomTest: ✅, MockServerPriorityTest: ✅）
- [x] Mock Server 至少通过一次"start -> stop -> start"自动化验证。

## 阶段 4: UI 性能细节修正（1h - 1.5h）
- [x] **步骤 4.1**: 优化 `HighPerfResponseViewer.kt` 大响应 size 计算，避免不必要内存拷贝。
- [x] **步骤 4.2**: 修复超大响应场景后按钮可见性状态恢复逻辑。

**阶段回归命令**
- [x] `./gradlew test --tests "*QuickLookPanelTest" --tests "*CollectionsPanelPerformanceTest"`

**完成标准（DoD）**
- [x] 相关 UI 回归测试通过。
- [x] 无超大响应场景下的交互状态残留。

## 阶段 5: 总体验收与归档（0.5h - 1h）
- [x] **步骤 5.1**: 运行全量测试并确认通过。— **330/330 tests PASSED** (2026-03-05)
- [~] **步骤 5.2**: 更新本计划执行记录（每项附结果/命令摘要）。
- [ ] **步骤 5.3**: 归档轨道。

**全量命令**
- [x] `./gradlew test` → BUILD SUCCESSFUL, 330 tests, 0 failed

**完成标准（DoD）**
- [x] 全量测试通过。
- [ ] 轨道状态与 `conductor/tracks.md` 保持一致。
