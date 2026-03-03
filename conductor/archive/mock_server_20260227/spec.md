# Technical Specification - Mock Server Support

## 1. 目标 (Goals)
在插件内部实现一个轻量级 Mock Server，允许开发人员在没有真实后端的情况下模拟 API 响应。

## 2. 核心功能 (Core Features)
- **多条件匹配**: 支持基于 URL 路径、HTTP 方法、请求头和请求体 (JSON/Text) 的匹配逻辑。
- **自定义响应**: 允许配置响应码、响应头、延迟 (Latency) 以及响应体。
- **内置引擎**: 使用内置的简单 HTTP 服务器（如基于 Sun Http Server 或 OkHttp MockWebServer）。
- **动态脚本**: 支持在 Mock 响应中使用脚本动态生成数据（可选增强）。

## 3. 技术路线 (Technical Approach)
- 在 IDE 进程中启动一个长驻的 HTTP Server 实例。
- 维护一个 `MockRule` 映射表。
- 集成到 `Collections` 中，允许用户右键请求“创建 Mock”。
