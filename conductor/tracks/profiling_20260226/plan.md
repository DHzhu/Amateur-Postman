# Implementation Plan - Phase 5: 性能分析 (Profiling)

## Phase 1: Metric Collection Core ✅ PENDING
- [ ] Task: 定义 `HttpProfilingData` 模型类来存储各阶段耗时
- [ ] Task: 实现 OkHttp `EventListener` 的自定义子类 `AmEventListener` 以收集时间戳
- [ ] Task: 更新 `HttpRequestServiceImpl` 以支持在请求发送时挂载该 `EventListener`
- [ ] Task: 编写测试 `AmEventListenerTest` 验证各阶段时间采集逻辑 (TDD: Red-Green-Refactor)
- [ ] Task: Conductor - User Manual Verification 'Metric Collection Core' (Protocol in workflow.md)

## Phase 2: Profiling UI Panel ✅ PENDING
- [ ] Task: 创建 `ProfilingPanel` 类 (基于 JBTable 或自定义渲染) 展示性能数据
- [ ] Task: 在 `PostmanToolWindowPanel` 的响应区域中添加 "Profiling" 标签页
- [ ] Task: 实现数据从 `HttpResponse` 到 `ProfilingPanel` 的绑定逻辑
- [ ] Task: 编写 UI 渲染测试或验证代码路径 (TDD: Red-Green-Refactor)
- [ ] Task: Conductor - User Manual Verification 'Profiling UI Panel' (Protocol in workflow.md)

## Phase 3: Refinement and Integration ✅ PENDING
- [ ] Task: 优化数据展示（如添加彩色条形图或可视化进度条表示耗时比例）
- [ ] Task: 处理特殊情况（如请求取消、连接重用时的 DNS 跳过等）
- [ ] Task: 编写集成测试验证端到端流程 (TDD: Red-Green-Refactor)
- [ ] Task: Conductor - User Manual Verification 'Refinement and Integration' (Protocol in workflow.md)
