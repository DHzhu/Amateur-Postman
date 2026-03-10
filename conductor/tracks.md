# Project Tracks

This file tracks all major tracks for the project. Each track has its own detailed plan in its respective folder.

## Active Tracks
(Currently none)

## Next Up
(Currently none)

## Archive

- [x] **Track: Protocol Expansion (WebSocket & gRPC Streaming)** [DONE]
  *Link: [./archive/protocol_expansion_streaming_20260309/](./archive/protocol_expansion_streaming_20260309/)*
  *Description: 扩展 WebSocket 连接与 gRPC (Server/Client/Bi-Di) 流式协议支持，包含高性能消息渲染、Disposable 资源管理、单元测试与集成测试。*
  *Completed: 2026-03-10*

- [x] **Track: Protocol Expansion (gRPC) Support** [DONE]
  *Link: [./archive/grpc_support_20260305/](./archive/grpc_support_20260305/)*
  *Description: 引入 gRPC 协议支持，实现 Proto 解析、Unary Call 调用及 UI 适配。*
  *Completed: 2026-03-06*

- [x] **Track: Hotfix: Mock Server, UI & Test Infrastructure** [DONE]
  *Link: [./archive/hotfix_mock_ui_tests_20260304/](./archive/hotfix_mock_ui_tests_20260304/)*
  *Description: 修复 GraalVM 跨 ScriptContext 对象迁移导致的 unexpected interop primitive 错误，修复 JUnit5 测试基建，330/330 测试通过。*
  *Completed: 2026-03-05*

- [x] **Track: UI/UX Refinement (Virtual Scrolling & High-Perf JSON Viewer)** [DONE]
  *Link: [./archive/ui_refinement_20260303/](./archive/ui_refinement_20260303/)*
  *Description: 集合树高性能渲染、HighPerfResponseViewer 组件、主题感知颜色、Collection Runner 批量操作。*
  *Completed: 2026-03-04*

- [x] **Track: Variable Scopes Visualization** [DONE]
  *Link: [./archive/variable_viz_20260303/](./archive/variable_viz_20260303/)*
  *Description: Environment Quick Look 悬浮窗，变量来源追踪，优先级高亮显示。*
  *Completed: 2026-03-04*

- [x] **Track: Scripting Power-up (Advanced API & pm.sendRequest)** [DONE]
  *Link: [./archive/scripting_powerup_20260303/](./archive/scripting_powerup_20260303/)*
  *Description: 修复 GraalVM 类型兼容性，集成 chai.js/ajv，实现 pm.sendRequest 桥接及 pm.response.to 快捷断言链。新增 38 个测试，全量 120 个测试。*
  *Completed: 2026-03-04*