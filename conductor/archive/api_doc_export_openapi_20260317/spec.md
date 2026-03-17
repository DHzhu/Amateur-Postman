# Track Specification: API Documentation Export (OpenAPI)

## 1. Goal
将现有的请求集合（Collection）导出为标准 OpenAPI Specification (OAS) 3.0.x/3.1.x (YAML/JSON)，实现从“调试工具”到“定义工具”的闭环。

## 2. Requirements

### R1: OpenAPI 导出核心
- 将 `Collection` 映射为 OpenAPI `Paths` 和 `Components`。
- 将 `HttpRequest` 的 URL, Method, Headers, Params, Body 转换为对应的 OpenAPI 操作定义。
- 支持基于响应历史（History）自动推断响应 Schema（可选）。

### R2: 格式支持
- 支持导出为 YAML（默认）和 JSON 格式。
- 支持 OpenAPI 3.0.0 作为首选版本。

### R3: UI/UX
- 在 Collection 的右键菜单或详情页增加 "Export as OpenAPI" 选项。
- 提供简单的导出配置：版本选择、文件格式、文档标题。

## 3. Technical Strategy
- 使用 `io.swagger.core.v3:swagger-models` 或 `swagger-parser` 中的模型构建文档。
- 实现 `OpenApiExporter` 服务类。
- 处理变量解析：将内部变量 `{{baseUrl}}` 映射为 OpenAPI 的 `servers` 配置或保持原样。

## 4. Security & Constraints
- 导出时默认移除敏感 Header（如 `Authorization`, `Cookie`），或由用户决定是否保留。
- 递归处理嵌套的文件夹结构，映射为 OpenAPI 的 `tags` 或通过路径前缀进行组织。
