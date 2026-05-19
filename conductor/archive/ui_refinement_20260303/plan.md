# Implementation Plan: UI/UX Refinement

## 阶段 1: 集合树性能基准与优化
- [x] **步骤 1.1**: 编写测试用例，生成一个包含 1000 个请求的 mock 集合数据。 (0.5h) ✅
- [x] **步骤 1.2**: 在 `CollectionsPanel` 中应用 `setLargeModel(true)` 优化，实现智能首层展开策略。 (2h) ✅
- [x] **步骤 1.3**: 验证在大数据量下展开/折叠节点无延迟。 (0.5h) ✅

## 阶段 2: 高性能 JSON 渲染
- [x] **步骤 2.1**: 创建 `HighPerfResponseViewer` 组件，实现超大文本分块显示与性能自适应。 (2h) ✅
- [x] **步骤 2.2**: 实现性能自适应模式：超过 5MB 文本自动提示关闭语法高亮。 (1h) ✅
- [x] **步骤 2.3**: 优化 `SyntaxHighlighter`，为大文件添加简化 tokenizer。 (2h) ✅

## 阶段 3: 主题与交互细节润色
- [x] **步骤 3.1**: 修正所有面板在 Light/Dark 模式下的边框色和按钮图标偏移。 (1h) ✅
- [x] **步骤 3.2**: 为 Collection Runner 添加批量操作功能 (Run Failed, Export Results, Clear)。 (1h) ✅

## 阶段 4: 性能测试与最终验证
- [x] **步骤 4.1**: 运行全量性能测试，所有测试通过。 (1h) ✅
- [x] **步骤 4.2**: 最终归档。 (0.5h) ✅

## 完成总结

### 新增文件
- `HighPerfResponseViewer.kt` - 高性能响应查看器，支持大文件和主题切换
- `CollectionsPanelPerformanceTest.kt` - 集合树性能基准测试

### 修改文件
- `CollectionsPanel.kt` - 应用 `setLargeModel(true)` 和智能展开策略
- `PostmanToolWindowPanel.kt` - 集成 `HighPerfResponseViewer`
- `SyntaxHighlighter.kt` - 添加大文件简化 tokenizer
- `CollectionRunnerDialog.kt` - 添加批量操作按钮

### 性能改进
- 集合树：支持 1000+ 请求流畅渲染
- JSON 渲染：>5MB 自动关闭语法高亮，>10MB 分块显示
- 主题支持：JBColor 实现无缝 Light/Dark 切换
