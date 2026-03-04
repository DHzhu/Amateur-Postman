# Amateur-Postman Changelog

所有对本项目的显著变更都将记录在此文件中。

本项目遵循 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) 规范。

## [Unreleased]
### Added
- **变量可视化 (Phase 3)**: 正在开发环境快速查看 (Environment Quick Look) 悬浮窗，支持多级变量来源追踪。
- **UI/UX 性能增强 (Phase 4)**: 规划中，重点解决大数据量响应下的渲染压力。

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
[0.3.0]: https://github.com/DHzhu/Amateur-Postman/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/DHzhu/Amateur-Postman/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/DHzhu/Amateur-Postman/releases/tag/v0.1.0
