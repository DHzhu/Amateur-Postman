# Track Specification: OpenAPI Deep Integration

## 1. Goal
实现 OpenAPI (Swagger) 规范的深度集成，支持从规范定义直接生成/同步请求集合，并利用 IDE 原生能力实现代码与请求的联动。

## 2. Requirements

### R1: OpenAPI 规范导入 (Import)
- 支持从本地文件（JSON/YAML）或远程 URL 导入 OpenAPI 2.0 (Swagger) 和 3.0.x/3.1.x 规范。
- 能够将 OpenAPI 的 `paths` 转换为插件内部的 `Collection` 和 `Request` 结构。
- 自动提取 `summary`, `description`, `parameters`, `requestBody` 并填充至请求模板。

### R2: 实时同步 (Sync)
- 允许现有的 Collection 绑定到一个 OpenAPI 定义文件。
- 支持“增量同步”或“全量覆盖”，保持 Collection 与 API 定义的一致性。
- 同步时保留用户自定义的 Pre-request/Test 脚本（如果可能）。

### R3: IDE 代码联动 (Code Linkage)
- **LineMarker**: 在 Spring Boot / JAX-RS 等框架的 Controller 方法旁显示 Gutter Icon。
- 点击图标后，自动在 Amateur-Postman 侧边栏定位或创建一个临时请求。
- 支持基于源代码路径匹配对应的 API Endpoint。

## 3. Technical Strategy

### OpenAPI 解析
- 考虑使用官方的 `swagger-parser` 库（Java/Kotlin 兼容性好）。
- 库 ID 调研：`io.swagger.parser.v3:swagger-parser`。

### 数据映射 (Mapping)
- `OpenAPI Path/Operation` -> `CollectionItem.Request`
- `Parameters (Query/Header/Path)` -> `HttpRequest.headers`, `HttpRequest.url` (variable resolution)
- `RequestBody` -> `HttpBody` (JSON/Multipart/Form-data)

### IDE 扩展点
- `com.intellij.codeInsight.daemon.LineMarkerProvider`: 用于实现 Controller 方法旁的图标。
- `com.intellij.openapi.actionSystem.AnAction`: 用于执行跳转和请求发起。

## 4. UI/UX Design
- **Import Dialog**: 增加 "Import from OpenAPI" 选项。
- **Collection Setting**: 增加 "Sync with OpenAPI" 配置项，支持定时检查或手动触发。
- **Gutter Icon**: 极简图标，与 IDEA 风格保持一致。

## 5. Security & Constraints
- 解析远程 URL 时需考虑超时和代理设置。
- 避免解析过大（>50MB）的 OpenAPI 文件导致内存溢出。
