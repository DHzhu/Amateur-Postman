# Implementation Plan - Phase 5: 性能分析 (Profiling)

## Phase 1: Metric Collection Core ✅ COMPLETED
- [x] Task: [TDD] 编写测试 `AmEventListenerTest`，定义对连接复用和完整连接场景下时间采集的预期行为
- [x] Task: 在 `HttpModels.kt` 中定义 `HttpProfilingData` 模型，支持可选的时间段（以兼容连接复用场景），并在 `HttpResponse` 中添加该字段
- [x] Task: 实现 OkHttp `EventListener` 的自定义子类 `AmEventListener`，安全地收集 DNS, TCP, SSL, TTFB 等时间戳
- [x] Task: 更新 `HttpRequestServiceImpl`，在请求执行时注入 `AmEventListener` 并将收集到的数据附加到返回的 `HttpResponse` 中
- [x] Task: Conductor - User Manual Verification 'Metric Collection Core' (Protocol in workflow.md)

## Phase 2: Profiling UI Panel ✅ COMPLETED
- [x] Task: [TDD] 编写 UI 绑定逻辑的单元测试，验证包含和不包含网络建立阶段的 `HttpProfilingData` 都能正确解析为展示模型
- [x] Task: 在 `PostmanToolWindowPanel` 的响应区域中新增 "Profiling" 标签页
- [x] Task: 创建基础的 `ProfilingPanel` 类，实现数据从 `HttpResponse.profilingData` 到 UI 的传递
- [x] Task: Conductor - User Manual Verification 'Profiling UI Panel' (Protocol in workflow.md)

## Phase 3: Visual Timeline (Waterfall) & Refinement ✅ COMPLETED
- [x] Task: 开发自定义的瀑布流/甘特图组件，使用彩色条形图直观展示各阶段耗时比例
- [x] Task: 处理连接复用场景的 UI 展示（明确标识 DNS/TCP 耗时为 0 是因为 Connection Reused）
- [x] Task: 编写集成测试，验证整个工具窗口在发送请求后能正确渲染出性能图表
- [x] Task: Conductor - User Manual Verification 'Visual Timeline & Refinement' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review suggestions 84d4ad3
