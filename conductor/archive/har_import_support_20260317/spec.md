# Track Specification: HAR (HTTP Archive) Import Support

## 1. Goal
支持导入标准 HTTP Archive (HAR) 1.2 格式文件，方便用户将浏览器捕获的请求快速转化为 Amateur-Postman 的请求集合。

## 2. Requirements

### R1: HAR 文件解析
- 支持读取并解析 `.har` (JSON) 文件。
- 提取 `log.entries` 中的请求元数据（Method, URL, Headers, Query Params, Body）。
- 提取响应元数据（Status, Headers, Body）以备后续参考。

### R2: 数据转换与导入
- 将 HAR Entry 转换为内部的 `HttpRequest` 和 `CollectionItem`。
- 支持批量导入 HAR 中的所有请求，或由用户选择性导入。
- 自动处理 Body 格式（JSON, Text, Multipart）。

### R3: UI/UX
- 在 "Import" 菜单中增加 "Import from HAR" 选项。
- 提供预览窗口，展示 HAR 文件中的请求列表及详情。

## 3. Technical Strategy
- 使用 Jackson 解析 JSON 格式的 HAR 文件。
- 实现 `HarImporter` 服务类，封装转换逻辑。
- 调研是否需要第三方库（如 `browser-mob-proxy` 相关的 HAR 模型）或直接使用简单的 POJO。

## 4. Security & Constraints
- 对超大 HAR 文件（如包含大量 Base64 图片/视频流）进行大小限制（建议 <20MB）。
- 处理潜在的隐私数据，如 Cookie 和 Authorization Header。
