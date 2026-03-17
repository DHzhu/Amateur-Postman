# Track Plan: OpenAPI Exporter Audit & Fix

## Phase 1: Tech Stack Rebuild (Execution)
- [x] 1.1 引入 `io.swagger.core.v3:swagger-models` 与 `swagger-core` 核心依赖 (如缺失) (5658ef2)
- [x] 1.2 重写 `OpenApiExporter.buildOpenApiDoc`，使用 `OpenAPI` 模型对象 (5658ef2)
- [x] 1.3 迁移 URL/Path 参数转换逻辑到新模型 (5658ef2)
- [x] 1.4 更新单元测试以验证基础结构 (Red-Green-Refactor) (5658ef2)

## Phase 2: Feature Completion (Execution)
- [x] 2.1 实现 `Header` 导出逻辑，支持 `Parameters` 集合 (5658ef2)
- [x] 2.2 实现敏感信息过滤机制 (Filter for Auth/Cookies) (5658ef2)
- [x] 2.3 在 `OpenApiExportDialog` 中增加 "Include sensitive headers" 开关 (5658ef2)
- [x] 2.4 编写针对 Header 导出与过滤的专项单元测试 (5658ef2)

## Phase 3: Response Inference (Execution)
- [x] 3.1 接入 `RequestHistoryService` 以获取历史响应数据 (5658ef2)
- [x] 3.2 实现简易 `JsonToSchemaConverter` 转换器 (5658ef2)
- [x] 3.3 为导出的 Operation 自动注入推断的 Response Schema (可选) (5658ef2)
- [x] 3.4 验证在大规模数据下的导出性能 (5658ef2)

## Phase 4: Validation & Archiving (Validate)
- [~] 4.1 运行全量测试用例并确保 100% 通过
- [ ] 4.2 更新 `CHANGELOG.md` 记录此重大修复
- [ ] 4.3 按照新流程执行 Track 归档 (Archive & Cleanup)
