# Implementation Plan - Mock Server Support

## 📊 Code Review Results (2026-02-27)
- ✅ All tests pass (`./gradlew test`), No compiler errors
- **5 Code Issues Found:** 1 High, 2 Medium, 2 Low priority
- 📋 See [Memory Sync]: CodeReview:Feb27-2026, Plan:Amateur-Postman

---

## Phase 0: 代码质量关键修复 ⚠️ BLOCKING
*Must complete before new features to ensure stability.*

### High Priority
- [ ] Task: [FIX] 修复 GraalVM 脚本引擎并发竞态 (ScriptExecutionService)
  - *Issue:* Single-instance engine + multiple coroutines = context contamination
  - *Location:* L359, L449, L485
  - *Action:* Thread-safe wrapper (Mutex) or engine.eval() serialization
  - *Tests:* Concurrent pre/test script execution stress test

### Medium Priority  
- [ ] Task: [FIX] 错误响应隐藏堆栈跟踪 (HttpRequestServiceImpl)
  - *Issue:* Exception stack trace exposed in response body
  - *Location:* L127
  - *Action:* Log internally only, return user-friendly error message
  - *Tests:* Verify error response format + no sensitive details

- [ ] Task: [FIX] Multipart 文本字段跳过自定义 content-type (HttpRequestServiceImpl)
  - *Issue:* Constructed requestBody ignored in line 193-195
  - *Location:* L193-195
  - *Action:* Use requestBody with custom MIME type for text fields
  - *Tests:* Multipart request with text field + explicit content-type roundtrip

### Low Priority
- [ ] Task: [IMPROVE] 表单数据导出启发式算法不稳定 (PostmanExporter)
  - *Issue:* `body.contains("=")` false positives for plain text
  - *Location:* L259
  - *Suggestion:* Check content-type field or Postman body.mode explicitly

- [ ] Task: [IMPROVE] UI：性能时间线面板主题感知颜色 (ProfilingTimelinePanel)
  - *Issue:* Hardcoded RGB colors may have contrast issues in light themes
  - *Location:* L28-L29
  - *Suggestion:* Use IDE theme colors via UIManager

---

## Phase 1: 核心 Mock 引擎实现 ✅ PENDING
- [ ] Task: [TDD] 编写 MockServer 基础测试，验证端口启动和简单的路径路由匹配
- [ ] Task: 实现 `MockRule` 模型和 `MockServerManager` 服务
- [ ] Task: 集成 Sun Http Server 或 OkHttp MockWebServer 到插件生命周期
- [ ] Task: 实现基础的匹配算法 (Path + Method)
- [ ] Task: Conductor - User Manual Verification 'Core Mock Engine'

## Phase 2: 配置 UI 与持久化 ✅ PENDING
- [ ] Task: 创建 `MockServerPanel` 用于管理规则列表
- [ ] Task: 实现 Mock 规则的 XML 持久化存储
- [ ] Task: 在 Collection 树形菜单中添加 "Create Mock from Request" 操作
- [ ] Task: Conductor - User Manual Verification 'Mock UI & Persistence'

## Phase 3: 高级功能与优化 ✅ PENDING
- [ ] Task: 支持响应延迟模拟 (Latency)
- [ ] Task: 支持请求体精确/正则匹配
- [ ] Task: 编写集成测试，验证通过插件发送请求到本地 Mock 端口并获得预期响应
- [ ] Task: Conductor - User Manual Verification 'Advanced Mock Features'
