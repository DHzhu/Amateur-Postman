# Specification: UI/UX Refinement

## 目标
提高在极端场景（如 500+ 个接口的集合，10MB 以上的 JSON 响应）下的 UI 流畅度，减少内存占用和渲染延迟。

## 关键功能定义

### 1. 集合树的虚拟滚动 (Tree Virtual Scrolling)
- **挑战**: `JBTree` 在有数千个节点展开时会变得卡顿。
- **方案**: 确保使用的树模型是懒加载的，并确认 `JBTree` 的 `viewport` 滚动重用逻辑生效。优化 `CollectionsPanel` 的 `TreeModel`。

### 2. 高性能 JSON Viewer (Big File Optimization)
- **挑战**: 超大 JSON 在语法高亮时会由于全量渲染导致 UI 假死。
- **方案**:
    - **分片渲染**: 仅渲染当前视口（Viewport）内的文本。
    - **禁用重型功能**: 对于超过 5MB 的文件，自动切换到“纯文本/只读”模式。
    - **正则匹配优化**: 使用更高效的 JSON 解析引擎进行语法着色，避免正则表达式递归。

### 3. 一致性界面润色
- **细节**: 全局字体统一、颜色主题深度适配（Darcula 模式细节）、按钮点击反馈。

## 验收标准
- 即使加载 1000 个请求的 Collection，树列表依然能实现秒级滚动。
- 渲染 10MB JSON 响应时，内存增长控制在 50MB 以内，且 UI 不应无响应超过 500ms。
