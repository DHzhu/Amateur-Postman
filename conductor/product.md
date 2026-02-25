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
- **cURL Support**: 支持将请求导出为 cURL 命令或从 cURL 导入。

## Design Principles
- **Lightweight**: 核心功能聚焦，避免功能过载。
- **IDE Native**: 界面风格与 IntelliJ IDEA 高度统一，使用原生 UI 组件。
- **Instant Feedback**: 极速响应，操作反馈实时直观。

## Roadmap Highlights
- **Phase 1: 请求功能增强** (待进行): Raw 编辑器增强、文件上传 (Multipart)、GraphQL 支持。
- **Phase 4: 测试与自动化** (待进行): 响应断言测试、Pre-request 脚本、Collection Runner。
