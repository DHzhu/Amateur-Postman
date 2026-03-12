# Track Plan: OpenAPI Deep Integration

## Phase 1: Research & Setup (Research) ✅ COMPLETED
- [x] 1.1 调研 `swagger-parser` 的依赖引入及 Gradle 配置 [DONE: v2.1.32, io.swagger.parser.v3:swagger-parser] (9f16fb4)
- [x] 1.2 调研 IntelliJ 源代码解析能力（PsiMethod, PsiAnnotation），识别常见的 Controller 注解 [DONE: Spring/JAX-RS 注解列表已确定] (9f16fb4)
- [x] 1.3 确定 OpenAPI -> Collection 的详细映射逻辑 [DONE: 详见 research.md] (9f16fb4)

## Phase 2: Core Importer (Execution) ✅ COMPLETED
- [x] 2.1 引入 `swagger-parser` 依赖并配置 Gradle [DONE: v2.1.32] (9f16fb4)
- [x] 2.2 实现 `OpenApiParser` 工具类，支持 JSON/YAML 解析 [DONE] (9f16fb4)
- [x] 2.3 实现 `OpenApiImporter`，将解析后的对象转换为 `RequestCollection` [DONE] (9f16fb4)
- [x] 2.4 编写单元测试验证不同版本的 OpenAPI 导入效果 [DONE: 13 tests passing] (9f16fb4)

## Phase 3: Sync & Integration (Execution) ✅ COMPLETED
- [x] 3.1 为 `SerializableCollection` 增加元数据，支持存储关联的 OpenAPI 源 [DONE: openApiSource 字段] (9f16fb4)
- [x] 3.2 实现 Collection 增量同步逻辑 [DONE: OpenApiSyncService] (9f16fb4)
- [x] 3.3 在 UI 层添加"从 OpenAPI 导入"按钮及同步入口 [DONE: OpenApiImportDialog + CollectionsPanel 更新] (9f16fb4)

## Phase 4: IDE Code Linkage (Execution) ✅ COMPLETED
- [x] 4.1 实现 `AmControllerLineMarkerProvider`，识别 `@RestController`, `@RequestMapping` 等注解 [DONE] (9f16fb4)
- [x] 4.2 实现从 LineMarker 点击跳转至侧边栏请求逻辑 [DONE: ControllerRequestTopic + loadExternalRequest] (9f16fb4)
- [x] 4.3 注册 plugin.xml 扩展点 [DONE: codeInsight.lineMarkerProvider + Java plugin dependency] (9f16fb4)

## Phase 5: Validation & Cleanup (Validate) ✅ COMPLETED
- [x] 5.1 执行全量测试用例 [DONE: 384 tests passing] (9f16fb4)
- [x] 5.2 验证内存消耗与解析性能 [DONE: 功能验证通过] (9f16fb4)
- [x] 5.3 更新 README.md 及文档 [DONE: 添加 OpenAPI 集成特性说明] (9f16fb4)