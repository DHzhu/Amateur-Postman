# Project Tracks

This file tracks all major tracks for the project. Each track has its own detailed plan in its respective folder.

## Active Tracks

(To be planned)

## Next Up

(To be planned)

## Archive

- [x] **Track: 升级 IDEA 版本支持至 2026.2** [DONE]
  *Link: [./archive/idea_2026_2_support_20260812/](./archive/idea_2026_2_support_20260812/)*
  *Description: 更新插件配置以支持 IntelliJ IDEA 2026.2 版本及相关兼容性修复。*
  *Started: 2026-08-12*
  *Completed: 2026-08-12*

- [x] **Track: 代码审查问题修复（第三轮）** [DONE]
  *Link: [./tracks/code_review_issues_fix_20260610/](./tracks/code_review_issues_fix_20260610/)*
  *Description: 修复第三轮代码审查中发现的 5 项严重问题，包括随机数溢出、Mock 延迟同步阻塞、JS 全局锁网络 IO 瓶颈、OAuth2 CSRF 隐患与配置更新竞态条件。*
  *Started: 2026-06-10*
  *Completed: 2026-06-10*

- [x] **Track: Postman 风格 UI 重新设计** [DONE]
  *Link: [./archive/ui_redesign_postman_style_20260519/](./archive/ui_redesign_postman_style_20260519/)*
  *Description: 重新设计 HTTP、WebSocket、gRPC 三个 Tab 的窗口布局，参照 Postman 样式：历史窗口默认关闭、参数表格行内操作按钮、动态尺寸调整、整体布局美化。*
  *Started: 2026-05-19*
  *Completed: 2026-05-19*

- [x] **Track: 代码审查问题修复（第二轮）** [DONE]
  *Link: [./archive/code_review_fix_20260519_v2/](./archive/code_review_fix_20260519_v2/)*
  *Description: 全面代码审查后的问题修复，涵盖线程安全、资源泄漏、空安全、安全漏洞、逻辑 bug、Darcula 主题兼容等 21 项问题。*
  *Started: 2026-05-19*
  *Completed: 2026-05-19*

- [x] **Track: 全面代码审查修复（第一轮）** [DONE]
  *Link: [./archive/code_review_fix_20260519/](./archive/code_review_fix_20260519/)*
  *Description: 全面代码审查后的问题修复，涵盖安全漏洞、资源泄漏、线程安全、测试质量等 29 项问题。*
  *Started: 2026-05-19*
  *Completed: 2026-05-19*

- [x] **Track: Variable Resolver Logic Optimization** [DONE]
  *Link: [./archive/variable_resolver_optimization_20260318/](./archive/variable_resolver_optimization_20260318/)*
  *Description: 重构变量解析算法，从正则表达式替换优化为单次 StringBuilder 扫描，消除大 Body 场景下的内存抖动与性能瓶颈。新增 1MB 性能基准测试及边界用例。*
  *Completed: 2026-03-18*

- [x] **Track: UI Performance - Migration to IntelliJ Editor** [DONE]
  *Link: [./archive/ui_performance_editor_upgrade_20260318/](./archive/ui_performance_editor_upgrade_20260318/)*
  *Description: 将响应查看器从 JTextPane 迁移至 IntelliJ 原生 Editor 组件，支持虚拟滚动、异步高亮及超大响应（100MB+）的高性能渲染。*
  *Completed: 2026-03-18*

- [x] **Track: JSON Infrastructure - Full Migration to Jackson** [DONE]
  *Link: [./archive/json_infra_jackson_migration_20260318/](./archive/json_infra_jackson_migration_20260318/)*
  *Description: 彻底移除项目中的 Gson 依赖，全线迁移至 Jackson (with Kotlin Module)，实现统一的 JsonService 单例管理，提升大数据量下的序列化性能与 Kotlin 类型安全性。*
  *Completed: 2026-03-18*

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
