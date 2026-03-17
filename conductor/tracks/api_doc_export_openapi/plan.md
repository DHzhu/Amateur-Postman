# Track Plan: API Documentation Export (OpenAPI)

## Phase 1: Research & Setup (Research)
- [x] 1.1 调研 Collection -> OpenAPI 3.0 的字段映射最佳实践 (95a324f)
- [x] 1.2 调研 `swagger-core` 模型构建库的引入 (95a324f)
- [x] 1.3 确定嵌套文件夹到 Tags 的转换策略 (95a324f)

## Phase 2: Export Engine (Execution)
- [x] 2.1 引入 `swagger-models` 依赖并配置 Gradle (95a324f)
- [x] 2.2 实现 `OpenApiExporter`，负责从 `Collection` 构建 OpenAPI 对象 (95a324f)
- [x] 2.3 实现 YAML/JSON 序列化逻辑 (95a324f)
- [x] 2.4 编写单元测试验证生成文档的合法性 (95a324f)

## Phase 3: UI Integration (Execution)
- [x] 3.1 实现导出配置弹窗 `OpenApiExportDialog` (95a324f)
- [x] 3.2 在 `CollectionsPanel` 的右键菜单和工具栏添加导出操作 (95a324f)
- [x] 3.3 实现文件保存对话框集成 (95a324f)

## Phase 4: Validation & Cleanup (Validate)
- [x] 4.1 验证导出的文档在外部工具（如 Swagger Editor）中的兼容性 (558f6d5)
- [x] 4.2 执行全量测试用例 (558f6d5)
- [x] 4.3 更新 README.md 及文档 (558f6d5)
