# Product Definition - Amateur-Postman

## Vision

Amateur-Postman 旨在为开发者提供一个集成在 IntelliJ IDEA 内部的、极简且高效的 HTTP 客户端。它填补了内置 HTTP 客户端过于繁琐与外部工具（如 Postman）切换成本高之间的空白，让简单的 API 测试变得触手可得。

## Target Users

- 需要快速验证 API 响应的后端开发者。
- 习惯在 IDE 内完成所有开发任务，追求“无缝切换”体验的工程师。
- 寻找比 IntelliJ 内置 .http 文件更直观、具有图形界面的替代方案的用户。

## Core Features

- **HTTP Request Builder**: 支持 GET, POST, PUT, DELETE 等常用方法，可自定义 Headers, Query Parameters 和 Body (JSON/Text)。
- **Response Viewer**: 自动格式化响应内容（如 JSON 语法高亮），显示响应状态码、执行时间和大小。
- **Collection Management** (✅已完成): 组织和保存常用的请求到集合中，支持多层文件夹嵌套。
- **Environment Support** (✅已完成): 支持环境变量（如 Base URL, Auth Tokens）和全局变量，支持多种随机函数（$timestamp, $uuid, $randomInt 等）。
- **Request History**: 自动保存请求历史，支持快速回溯和重新发起。
- **Postman Integration** (✅已完成): 支持导入和导出 Postman Collection v2.1 格式。
- **cURL Support** (✅已完成): 支持将请求导出为 cURL 命令或从 cURL 导入。
- **Advanced Request Support** (✅已完成): 支持 Raw 编辑器多类型支持、Multipart/form-data 文件上传、GraphQL 支持。
- **Testing & Automation** (✅已完成): 支持 Pre-request 脚本、Response Assertions (Tests) 以及 Collection Runner (批量运行)。
- **Advanced Protocol Support** (🚧进行中): 扩展对 gRPC (Unary Call) 及后续流式协议的支持。

## Design Principles

- **Lightweight**: 核心功能聚焦，避免功能过载。
- **IDE Native**: 界面风格与 IntelliJ IDEA 高度统一，使用原生 UI 组件。
- **Instant Feedback**: 极速响应，操作反馈实时直观。

## Roadmap Highlights

- **Phase 1: 请求功能增强** (✅已完成): Raw 编辑器增强、文件上传 (Multipart)、GraphQL 支持。
- **Phase 2: 脚本引擎增强** (✅已完成): 集成 chai.js/ajv，支持 `pm.sendRequest` 异步调用。
- **Phase 3: 变量可视化** (✅已完成): Environment Quick Look 悬浮窗，支持变量来源与优先级追踪。
- **Phase 4: 测试与自动化** (✅已完成): 响应断言测试、Pre-request 脚本、Collection Runner。
- **Phase 5: 性能分析与 Mock Server** (✅已完成): 时序瀑布流、内置 Netty Mock Server。
- **Phase 6: 脚本引擎增强与 UI 优化** (✅已完成): 虚拟滚动、GraalVM JS 深度兼容、High-Perf Viewer。
- **Phase 7: 协议扩展 (gRPC)** (🚧进行中): 实现 Proto 动态解析与一元 RPC 调用。

## Next Steps

- **WebSocket 支持**: 实现长连接调试与消息历史记录。
- **外部脚本库动态加载**: 允许用户 `require` 自定义 JS 库。

