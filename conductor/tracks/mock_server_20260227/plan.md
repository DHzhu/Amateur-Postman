# Implementation Plan - Mock Server Support

## 📊 Code Review Results (2026-02-27)
- ✅ All tests pass (`./gradlew test`), No compiler errors
- **5 Code Issues Found:** 1 High, 2 Medium, 2 Low priority
- 📋 See [Memory Sync]: CodeReview:Feb27-2026, Plan:Amateur-Postman

---

## Phase 0: 代码质量关键修复 ✅ COMPLETED (2026-03-02)
*All fixes completed and tested.*

### High Priority
- [x] Task: [FIX] 修复 GraalVM 脚本引擎并发竞态 (ScriptExecutionService)
  - *Fixed:* Added Mutex to serialize engine.eval() calls
  - *Tests:* Added concurrent stress tests in ScriptExecutionServiceTest

### Medium Priority  
- [x] Task: [FIX] 错误响应隐藏堆栈跟踪 (HttpRequestServiceImpl)
  - *Fixed:* User-friendly error messages, full stack trace logged internally

- [x] Task: [FIX] Multipart 文本字段自定义 content-type (HttpRequestServiceImpl)
  - *Fixed:* Use addPart with explicit headers for custom content-type text fields

### Low Priority
- [x] Task: [IMPROVE] 表单数据导出启发式算法不稳定 (PostmanExporter)
  - *Fixed:* Check content-type explicitly instead of body.contains("=")
  - *Added:* BodyType.FORM_URLENCODED enum value

- [x] Task: [IMPROVE] UI：性能时间线面板主题感知颜色 (ProfilingTimelinePanel)
  - *Fixed:* Replaced hardcoded RGB with JBColor (dark/light theme aware)

---

## Phase 1: 核心 Mock 引擎实现 ✅ COMPLETED (2026-03-02)
- [x] Task: [TDD] 编写 MockServer 基础测试，验证端口启动和简单的路径路由匹配
  - *Created:* MockServerManagerTest with 20+ test cases
- [x] Task: 实现 `MockRule` 模型和 `MockServerManager` 服务
  - *Created:* MockModels.kt, MockServerManager.kt
- [x] Task: 集成 Sun Http Server 到插件生命周期
  - *Used:* com.sun.net.httpserver.HttpServer (JDK built-in)
- [x] Task: 实现基础的匹配算法 (Path + Method)
  - *Implemented:* Exact path + HTTP method matching

## Phase 2: 配置 UI 与持久化 ✅ COMPLETED (2026-03-02)
- [x] Task: 创建 `MockServerPanel` 用于管理规则列表
  - *Created:* MockServerPanel.kt, MockRuleDialog.kt
  - *Features:* Server start/stop, rules table, add/edit/delete/toggle rules
- [x] Task: 实现 Mock 规则的 XML 持久化存储
  - *Used:* IntelliJ PersistentStateComponent
  - *Storage:* amateur-postman-mock.xml
- [x] Task: 在 Collection 树形菜单中添加 "Create Mock from Request" 操作
  - *Location:* CollectionsPanel context menu
  - *Features:* Auto-extract path, pre-fill method and body

## Phase 3: 高级功能与优化 ✅ COMPLETED (2026-03-02)
- [x] Task: 支持响应延迟模拟 (Latency)
  - *Implemented:* In Phase 1 (delayMs field in MockRule)
- [x] Task: 支持请求体精确/正则匹配
  - *Added:* BodyMatcher, BodyMatchMode (NONE, EXACT, CONTAINS, REGEX)
  - *UI:* MockRuleDialog supports body matching configuration
- [x] Task: 编写集成测试，验证通过插件发送请求到本地 Mock 端口并获得预期响应
  - *Tests:* MockServerManagerTest (20+ tests), MockServerBodyMatchingTest (10 tests)
  - *Status:* 272 tests passing
