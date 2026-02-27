# Technical Specification - Enhanced Scripting API

## 1. 目标 (Goals)
扩展现有的脚本执行引擎（GraalVM JS），提供更完整的 Postman 兼容 API，支持更复杂的请求前置处理和测试断言。

## 2. 核心 API 扩展 (API Extensions)
### 2.1 变量作用域 (Variable Scopes)
- `pm.globals`: 全局变量支持（跨集合共享）。
- `pm.collectionVariables`: 集合级变量支持。
- `pm.environment`: (已存在) 增强对私有/公开环境的处理。
- `pm.variables.get()`: 按照优先级（Global -> Collection -> Environment -> Data）解析变量。

### 2.2 请求/响应对象 (Request/Response Objects)
- `pm.request`: 访问当前请求的 URL, Method, Headers, Body。
- `pm.response`:
    - `pm.response.json()`: 自动解析 JSON。
    - `pm.response.text()`: 获取原始文本。
    - `pm.response.headers.get()`: 获取响应头。
    - `pm.response.responseTime`: 记录响应耗时（结合 Profiling 数据）。

### 2.3 常用工具库 (Utility Libraries)
- `crypto-js`: 支持常用的加密算法（MD5, SHA, AES）。
- `ajv`: 支持 JSON Schema 验证。
- `atob`/`btoa`: Base64 编解码。

## 3. 技术实现 (Technical Implementation)
- 使用 GraalVM 的 `Context.newBuilder().allowAllAccess(true).build()` 注入 Kotlin 对象。
- 将变量存储逻辑从 `VariableResolver` 扩展到多层级存储结构。
- 引入外部 JS 库的预加载机制。
