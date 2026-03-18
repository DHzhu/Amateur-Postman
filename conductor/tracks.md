# Project Tracks

This file tracks all major tracks for the project. Each track has its own detailed plan in its respective folder.

## Active Tracks

- [ ] **Track: UI Performance - Migration to IntelliJ Editor**
  *Link: [./tracks/ui_performance_editor_upgrade/](./tracks/ui_performance_editor_upgrade/)*
  *Description: 将响应查看器从 JTextPane 迁移至 IntelliJ 原生 Editor 组件，支持虚拟滚动、异步高亮及超大响应（100MB+）的高性能渲染。*

- [ ] **Track: JSON Infrastructure - Full Migration to Jackson**
  *Link: [./tracks/json_infra_jackson_migration/](./tracks/json_infra_jackson_migration/)*
  *Description: 彻底移除项目中的 Gson 依赖，全线迁移至 Jackson (with Kotlin Module)，实现统一的 JsonService 单例管理，提升大数据量下的序列化性能与 Kotlin 类型安全性。*

- [ ] **Track: Variable Resolver Logic Optimization**
  *Link: [./tracks/variable_resolver_optimization/](./tracks/variable_resolver_optimization/)*
  *Description: 重构变量解析算法，从正则表达式替换优化为单次扫描 StringBuilder 替换，解决在大 Body 场景下的内存抖动与性能瓶颈。*

## Next Up

(To be planned)

## Archive

- [x] **Track: OpenAPI Exporter Audit & Fix** [DONE]
  *Link: [./archive/openapi_exporter_audit_fix_20260317/](./archive/openapi_exporter_audit_fix_20260317/)*
  *Description: 修复 OpenAPI 导出功能的技术偏差与功能缺失（补齐 Headers 导出、敏感头过滤及改用 swagger-models 模型库）。*
  *Completed: 2026-03-17*

- [x] **Track: API Documentation Export (OpenAPI)** [DONE]
  *Link: [./archive/api_doc_export_openapi_20260317/](./archive/api_doc_export_openapi_20260317/)*
  *Description: 支持将 Amateur-Postman 的 Collection 导出为标准 OpenAPI 3.0 规范文档，支持 YAML/JSON 双格式，文件夹层级映射 Tags，变量转换路径参数。*
  *Completed: 2026-03-17*

- [x] **Track: HAR (HTTP Archive) Import Support** [DONE]
  *Link: [./archive/har_import_support_20260317/](./archive/har_import_support_20260317/)*
  *Description: 支持从浏览器或其他抓包工具导出的 .har 文件导入 HTTP 请求历史及集合。*
  *Completed: 2026-03-17*

- [x] **Track: Authentication Framework & OAuth 2.0 Support** [DONE]
  *Link: [./archive/auth_framework_oauth2_20260312/](./archive/auth_framework_oauth2_20260312/)*
  *Description: 引入统一的 HTTP 请求认证框架，实现 OAuth 2.0 各类授权流程（授权码、客户端凭据、密码、Implicit）及令牌自动刷新机制，支持集合级认证继承。*
  *Completed: 2026-03-12*

- [x] **Track: OpenAPI Deep Integration** [DONE]
  *Link: [./archive/openapi_integration_20260311/](./archive/openapi_integration_20260311/)*
  *Description: 实现 OpenAPI 规范导入、实时同步以及 IDE 代码与请求的深度联动。*
  *Completed: 2026-03-12*

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
