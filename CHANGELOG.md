# Amateur-Postman Changelog

所有对本项目的显著变更都将记录在此文件中。

本项目遵循 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) 规范。

## [Unreleased]

## [0.4.1] - 2026-03-17
### Changed
- **OpenAPI 导出器重构**: 弃用自定义 POJO 拼装，改用 `io.swagger.v3.oas.models` 官方模型库，序列化由 `swagger-core` 的 `Yaml/Json.pretty()` 接管，与导入器保持技术一致性。

### Added
- **Request Headers 导出**: 导出操作现支持将 HTTP 请求头映射为 OpenAPI `HeaderParameter`，完整保留自定义业务头信息。
- **敏感信息过滤**: 导出时默认过滤 `Authorization`、`Cookie`、`Set-Cookie` 等敏感头字段，防止凭证泄露到公开 API 文档。
- **"Include sensitive headers" 开关**: 导出对话框新增勾选项，用户可按需决定是否包含敏感请求头。
- **响应历史推断**: `exportCollection` 支持传入 `RequestHistoryService`，自动从历史记录中查找匹配请求的最近成功响应，使用真实状态码（而非硬编码 "200 OK"）。
- **JsonToSchemaConverter**: 新增简易 JSON → OpenAPI Schema 转换工具，支持对象、数组、原始类型的结构推断。

## [0.4.0] - 2026-03-17
### Added
- **API 导出支持 (OpenAPI)**: 支持将集合导出为 OpenAPI 3.0.3 规范（YAML/JSON），自动映射文件夹层级为 Tags。
- **HAR 导入支持**: 支持导入 .har 文件，具备静态资源过滤、Host 分组及选择性导入预览界面。
- **统一认证框架**: 引入 Authentication 继承模型，全面支持 OAuth 2.0 (Authorization Code, Client Credentials, etc.) 流程及令牌自动刷新。
- **OpenAPI 深度集成**: 支持 OpenAPI 规范的导入、实时同步以及 IDE 代码与请求的深度联动。
- **协议扩展 (流式)**: 支持 WebSocket 消息收发及 gRPC (Server/Client/Bi-Di) 流式调用，提供高性能消息列表展示。
- **协议扩展 (gRPC)**: 引入 gRPC 基础支持，支持解析 .proto 文件并进行 Unary 调用。
- **变量可视化 (Quick Look)**: 新增 Environment Quick Look 悬浮窗，支持变量来源追踪与优先级高亮。
- **UI/UX 性能优化**: 引入虚拟滚动技术支持大规模集合展示，优化 High-Perf Response Viewer 以处理 10MB+ 响应。

## [0.3.0] - 2026-03-03
### Added
- **脚本引擎增强**: 集成 GraalVM JS，全面支持 `pm.sendRequest` (异步)、`chai.js` 断言及 `CryptoJS`。
- **Mock Server 强化**: 支持规则优先级匹配、HTTP 方法过滤、大报文 OOM 保护及多端口管理。
- **测试覆盖**: 新增 120+ 脚本执行测试与 288+ Mock Server 稳定性测试。
### Changed
- **测试框架迁移**: 完成从 JUnit 4 到 **JUnit 5** 的全量迁移，覆盖 266 个验证点。

## [0.2.0] - 2026-02-25
### Added
- **多级变量系统**: 实现 Global, Environment, Collection 三层作用域解析。
- **请求历史管理**: 支持按日期归档的历史记录持久化。
- **GraphQL 支持**: 初步支持 GraphQL 请求体预览与发送。

## [0.1.0] - 2026-02-15
### Added
- **核心能力**: 支持基础 GET/POST/PUT/DELETE 请求发送。
- **集合导出/导入**: 支持 Postman 格式 (v2.1) 的导入与基本导出。
- **基础 UI**: 基于 IntelliJ ToolWindow 实现的极简测试面板。

---
[0.4.0]: https://github.com/DHzhu/Amateur-Postman/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/DHzhu/Amateur-Postman/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/DHzhu/Amateur-Postman/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/DHzhu/Amateur-Postman/releases/tag/v0.1.0
