# Implementation Plan - Phase 1: Request Enhancement

## Phase 1: Raw Editor Enhancement ✅ COMPLETED
- [x] Task: 扩展 HttpBody 数据模型以支持多种类型
- [x] Task: 更新 UI 以提供 Content-Type 下拉选择器
- [x] Task: 实现基于选择类型的语法高亮
- [x] Task: Conductor - User Manual Verification 'Raw Editor Enhancement' (Protocol in workflow.md)

**完成日期**: 2026-02-26
**提交**: df5aa8f, abce625

## Phase 2: Multipart/form-data ✅ COMPLETED
- [x] Task: 实现 Multipart 表格编辑器 UI
- [x] Task: 更新 HttpRequestService 以处理文件流发送
- [x] Task: 实现文件选择和预览逻辑
- [x] Task: Conductor - User Manual Verification 'Multipart Support' (Protocol in workflow.md)

**完成日期**: 2026-02-26
**修改文件**:
- `src/main/kotlin/com/github/dhzhu/amateurpostman/models/HttpModels.kt`: 添加 MULTIPART body type, MultipartPart 类
- `src/main/kotlin/com/github/dhzhu/amateurpostman/services/HttpRequestServiceImpl.kt`: 支持 multipart 请求构建
- `src/main/kotlin/com/github/dhzhu/amateurpostman/ui/PostmanToolWindowPanel.kt`: Multipart 编辑器 UI
- `src/main/kotlin/com/github/dhzhu/amateurpostman/ui/MultipartCellEditor.kt`: 自定义文件选择单元格编辑器

## Phase 3: GraphQL Support ✅ COMPLETED
- [x] Task: 实现 GraphQL 专用编辑器面板
- [x] Task: 实现 GraphQL 到标准 JSON Body 的转换逻辑
- [x] Task: Conductor - User Manual Verification 'GraphQL Support' (Protocol in workflow.md)

**完成日期**: 2026-02-26
**修改文件**:
- `src/main/kotlin/com/github/dhzhu/amateurpostman/models/HttpModels.kt`: 添加 GRAPHQL body type, GraphQLRequest 类
- `src/main/kotlin/com/github/dhzhu/amateurpostman/ui/GraphQLPanel.kt`: GraphQL 编辑器面板（Query, Variables, Operation Name）
- `src/main/kotlin/com/github/dhzhu/amateurpostman/ui/PostmanToolWindowPanel.kt`: 集成 GraphQL 编辑器
- `src/test/kotlin/com/github/dhzhu/amateurpostman/models/GraphQLRequestTest.kt`: GraphQL 请求转换测试
