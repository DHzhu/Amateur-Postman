# Track Plan: HAR (HTTP Archive) Import Support

## Phase 1: Research & Setup (Research)
- [x] 1.1 调研 HAR 1.2 规范及典型 JSON 结构 (c92540b)
- [x] 1.2 准备多种来源（Chrome, Firefox, Fiddler）的测试 .har 文件 (c92540b)
- [x] 1.3 确定 HAR -> Collection 的字段映射逻辑 (c92540b)

## Phase 2: Core Importer (Execution)
- [x] 2.1 实现 `HarParser` 工具类，基于 Gson 反序列化 (c92540b)
- [x] 2.2 实现 `HarConverter`，将 HAR Entries 转换为 `HttpRequest` (c92540b)
- [x] 2.3 编写单元测试验证不同来源的 HAR 导入效果 (c92540b)

## Phase 3: UI Integration (Execution)
- [x] 3.1 实现 `HarImportDialog`，支持文件选择与请求预览 (c92540b)
- [x] 3.2 在 `CollectionsPanel` 的导入菜单中注册入口 (c92540b)
- [x] 3.3 实现"选择性导入"功能（Checkbox 过滤） (c92540b)

## Phase 4: Validation & Cleanup (Validate)
- [x] 4.1 验证复杂 Body 类型（Multipart, URL-encoded）的解析准确性 (c92540b)
- [x] 4.2 执行全量测试用例，确保无回归 (c92540b)
- [x] 4.3 更新 README.md 特性说明 (fe86a5c)
