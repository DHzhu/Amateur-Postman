# Track Specification: OpenAPI Exporter Audit & Fix

## 1. Goal
修复 OpenAPI 导出功能的技术偏差与功能缺失，确保导出的文档符合 OpenAPI 3.0.3 标准并具备生产可用性。

## 2. Requirements

### R1: 技术栈重构
- 弃用自定义的 POJO 拼装方式，统一使用 `io.swagger.core.v3:swagger-models` 构建文档对象。
- 确保导出逻辑与导入逻辑（已使用 swagger-models）保持技术一致性。

### R2: 功能补齐
- **请求头导出**：必须支持导出 HTTP Request 中的 Headers。
- **敏感信息过滤**：导出时自动过滤或脱敏 `Authorization`, `Cookie`, `Set-Cookie` 等敏感字段。
- **响应 Schema 推断**：尝试从 `RequestHistoryService` 获取该请求的最近成功响应，推断基础的 JSON Schema（而非硬编码 "200 OK"）。

### R3: 兼容性与验证
- 修复 `{{variable}}` 变量在复杂 URL 场景下的转换准确性。
- 通过 Swagger Editor 验证生成文档的合法性。

## 3. Technical Strategy
- 利用 `io.swagger.v3.core.util.Yaml.pretty()` 和 `Json.pretty()` 进行序列化，替代 SnakeYAML/Gson 手动处理。
- 引入 `InferenceEngine` 工具类用于从响应 Body 推断 OpenAPI Schema。

## 4. Security & Constraints
- 导出对话框应增加“包含敏感信息”的勾选开关，默认关闭。
- 递归处理文件夹结构时，确保 Tag 路径的唯一性。
