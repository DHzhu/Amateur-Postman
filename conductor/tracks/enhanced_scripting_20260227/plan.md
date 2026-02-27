# Implementation Plan - Enhanced Scripting API

## Phase 1: 多级变量作用域支持 ✅ COMPLETED
- [x] Task: [TDD] 编写变量优先级测试，验证 Global -> Collection -> Environment 覆盖逻辑
- [x] Task: 在 `EnvironmentModels.kt` 中添加 `GlobalVariables` 和 `CollectionVariables` 模型
- [x] Task: 更新 `VariableResolver`，支持从多个作用域按优先级查找变量
- [x] Task: 在 `ScriptExecutionService` 中注入 `pm.globals` 和 `pm.collectionVariables` 接口
- [x] Task: Conductor - User Manual Verification 'Variable Scopes' (Protocol in workflow.md)

## Phase 2: 请求/响应对象增强 ✅ COMPLETED
- [x] Task: [TDD] 编写响应解析测试，验证 `pm.response.json()` 对复杂嵌套 JSON 的解析
- [x] Task: 在 `ScriptExecutionService` 中扩展 `pm.request` 访问权限
- [x] Task: 在 `pm.response` 对象中添加 `json()`, `text()`, `headers.get()` 方法
- [x] Task: 将 `HttpResponse.profilingData.totalTime` 绑定到 `pm.response.responseTime`
- [x] Task: Conductor - User Manual Verification 'Object Enhancement' (Protocol in workflow.md)

## Phase 3: 外部库引入与工具函数 ✅ COMPLETED
- [x] Task: 集成 `crypto-js` 脚本，并作为预加载资源注入到 GraalVM Context 中
- [x] Task: 实现原生的 `atob` 和 `btoa` 函数支持
- [x] Task: 编写集成测试，验证通过脚本生成 HMAC-SHA256 签名并注入到请求 Header 中
- [x] Task: Conductor - User Manual Verification 'External Libraries' (Protocol in workflow.md)
