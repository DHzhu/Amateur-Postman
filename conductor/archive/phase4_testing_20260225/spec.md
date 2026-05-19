# Specification - Phase 4: Testing and Automation

## Background
Amateur-Postman 目前具备了强大的请求发送能力。为了支持自动化测试流，我们需要引入测试断言（Tests）、请求前置处理（Pre-request Scripts）以及批量运行能力（Collection Runner）。

## Core Features

### 1. Pre-request Scripts
- **Goal**: 在请求发送前执行特定的逻辑（如计算签名、设置动态时间戳）。
- **Implementation**: 
  - 在 UI 中添加 "Pre-request" 标签页。
  - 集成基础的 JavaScript/Kotlin 脚本执行环境（初步考虑简单变量替换增强或基础脚本支持）。
  - 提供 `am.environment.set(key, value)` 等 API。

### 2. Response Assertions (Tests)
- **Goal**: 验证响应结果是否符合预期。
- **Implementation**:
  - 在 UI 中添加 "Tests" 标签页。
  - 提供预设的断言片段（如：Status code is 200, Response body contains string）。
  - 执行逻辑：请求完成后运行测试脚本，捕获并展示测试结果（Pass/Fail）。

### 3. Collection Runner
- **Goal**: 顺序执行整个集合或文件夹中的所有请求。
- **Implementation**:
  - 创建独立的 Runner 窗口/对话框。
  - 支持选择环境、设置迭代次数。
  - 展示实时运行进度和汇总统计报告（Total, Passed, Failed）。

## User Experience (UX)
- **Script Editor**: 使用带有基础高亮的文本区域。
- **Visual Feedback**: 测试结果应以清晰的 绿色 (Pass) / 红色 (Fail) 标识。
- **Integration**: 与现有的 Collection 树状结构深度集成，提供 "Run Collection" 右键菜单。

## Technical Constraints
- **Sandboxing**: 脚本执行应避免影响 IDE 稳定性。
- **Performance**: 批量运行不应阻塞 UI 线程，需利用 Coroutines 处理。
