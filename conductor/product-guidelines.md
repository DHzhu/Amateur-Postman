# Product Guidelines - Amateur-Postman

## Prose Style
- **Tone**: 简洁、专业、实用。
- **Language**: 界面文案首选英文（符合 JetBrains 插件市场惯例），提示消息和帮助文档支持中英双语。
- **Action-Oriented**: 使用动词引导的操作（如 "Send", "Save", "Import"），避免冗长的说明。

## User Experience (UX)
- **Zero Configuration**: 插件安装后应能立即使用，无需复杂的初始化配置。
- **Context Awareness**: 尽可能利用 IDE 当前的上下文（如从代码中提取 URL 或在当前项目内保存配置）。
- **Non-Intrusive**: 插件应以 Tool Window 形式存在，不应干扰用户的主要编码活动。
- **Error Gracefully**: 当 HTTP 请求失败或文件导入错误时，提供清晰的错误提示而不是简单的静默失败。

## Visual Identity
- **Standard UI Components**: 严格使用 IntelliJ Platform SDK 提供的 UI 组件（如 JBTable, JBTextField, Tree），确保外观与 IDE 完美融合。
- **Dark Mode Support**: 必须支持 Darcula 主题，确保在暗色模式下的可读性和对比度。
- **Icons**: 使用 JetBrains 官方图标库或与之风格一致的矢量图标。

## Functional Constraints
- **State Persistence**: 用户的请求、集合和环境变量必须在 IDE 重启后得以保留。
- **Performance**: 请求发送和响应渲染不应导致 IDE 界面卡顿。
