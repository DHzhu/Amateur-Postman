# 规格说明：Postman 风格 UI 重新设计

## 概述

参照 Postman 经过验证的 UI/UX 模式，重新设计三个协议 Tab 面板（HTTP、WebSocket、gRPC）。目标是更清洁、更紧凑、更专业的布局，在保持 IntelliJ 原生风格的同时借鉴 Postman 最佳交互模式。

## 设计原则（来自 product-guidelines.md）

- **IDE 原生**：仅使用 IntelliJ Platform SDK 组件
- **非侵入式**：紧凑布局，不浪费屏幕空间
- **Darcula 支持**：所有变更必须在亮色和暗色主题下正常工作

## 1. HTTP Tab（PostmanToolWindowPanel）

### 1.1 历史面板：默认关闭

**现状**：历史面板始终作为 250px 左侧边栏在水平分割器中可见。

**目标**：历史面板默认隐藏。顶部工具栏中的 "History" 切换按钮控制面板的显示/隐藏。按钮在历史可见时应有激活状态指示。

**实现**：
- 移除将历史嵌入为左侧组件的 `JBSplitter`
- 在顶部工具栏添加 `JButton("History")`（与 Import cURL、Export cURL、Save、Quick Look 并列）
- 使用滑动/停靠方案：切换时，历史面板作为左侧边栏插入主内容区的分割器中；关闭时，从布局中移除
- 在 `var historyVisible = false` 字段中跟踪切换状态

### 1.2 参数和 Headers 表格：行内操作

**现状**：表格在表格下方有单独的 "Add" 和 "Remove" 按钮面板（通过 `createTablePanel()`）。

**目标**：每行末尾有行内操作按钮。小的 "+" 按钮在当前行下方添加新行，"−" 按钮删除该行。表格下方有 "Add" 链接按钮。

**实现**：
- 为 `paramsTableModel` 和 `headersTableModel` 添加 "Actions" 列（列索引 = 最后一列）
- Actions 列的自定义 `TableCellRenderer`：渲染 "−"（删除）按钮图标
- Actions 列的自定义 `TableCellEditor`：包含点击时删除行的 `JButton` 的 `JPanel`
- 表格下方：简单的 "Add row" 链接按钮（不是完整的按钮面板）
- 编辑最后一行时自动添加空行（始终有空白行供输入）
- 从 `createTablePanel()` 中移除单独的按钮面板

### 1.3 动态表格尺寸

**现状**：表格在 `JBScrollPane` 中，具有固定的尺寸行为。

**目标**：表格高度应适应内容，显示所有行直到最大高度，然后滚动。

**实现**：
- 添加/删除行后，重新计算首选高度：`min(rowCount * rowHeight + headerHeight, maxHeight)`
- 动态设置滚动面板或表格的 `preferredSize`
- 最大高度：约 200px（约 8-10 行后开始滚动）
- 尺寸变化后在父组件上调用 `revalidate()`/`repaint()`

### 1.4 顶部工具栏整合

**现状**：两行 — 第一行有 Method + URL + Send，第二行有 Import cURL、Export cURL、Save、Quick Look。

**目标**：单个整洁的工具栏行。Method 选择器、URL 字段、Send 按钮为主要元素。其他按钮以较小/图标形式在辅助行或紧凑分组中。

## 2. WebSocket Tab（WebSocketPanel）

### 2.1 布局重新设计

**当前布局**：
```
URL: [ws://__________] [Connect] [Disconnect]
Headers: [table] [Add] [Remove]
--- 消息区域 ---
[Binary] [input] [Send] [Clear]
状态栏
```

**目标布局**（Postman 风格）：
```
┌─────────────────────────────────────────────────┐
│ [wss://] [________URL________] [Connect/Disconnect] │
├─────────────────────────────────────────────────┤
│ [Messages tab] [Headers tab]                     │
│ ┌─────────────────────────────────────────────┐ │
│ │ ↑ {"type":"subscribe"}                      │ │
│ │ ↓ {"type":"data","value":123}               │ │
│ └─────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────┤
│ [Binary] [___message input___] [Send] [Clear]  │
│ ● Connected | Messages: 42 | Sent: 10 | Recv: 32│
└─────────────────────────────────────────────────┘
```

**关键变更**：
- 顶部栏：URL + 协议 + 连接/断开在一个整洁的行中
- Headers 从始终可见改为 Tab 切换（Messages / Headers）
- 底部状态栏统一
- 更紧凑的间距

### 2.2 Headers 作为 Tab

**实现**：
- 用包含 "Messages" 和 "Headers" Tab 的 `JBTabbedPane` 替换始终可见的 Headers 区域
- Messages Tab：包含 `StreamMessageList`
- Headers Tab：包含带行内操作的 Headers 表格

## 3. gRPC Tab（GrpcEditorPanel）

### 3.1 布局重新设计

**当前布局**：
```
Proto: [________] [Browse] [Load]
Service: [▼] Method: [▼] Host: [__] Port: [__] [TLS] [Send] [Complete] [Cancel]
┌──────────────┬──────────────┐
│ Request      │ Response     │
│ [Body|Meta]  │ [Msg|Body|Meta] │
└──────────────┴──────────────┘
状态栏
```

**目标布局**（Postman 风格）：
```
┌─────────────────────────────────────────────────┐
│ [Proto: ________] [Browse] [Load]               │
│ Service: [▼____]  Method: [▼____]               │
│ Host: [______] Port: [____] [TLS]               │
├──────────────────────┬──────────────────────────┤
│ Request              │ Response                 │
│ [Body] [Metadata]    │ [Messages] [Body] [Trail]│
│                      │                          │
│ ┌──────────────────┐ │ ┌──────────────────────┐ │
│ │ {                │ │ │ 响应内容             │ │
│ │   "name": "..."  │ │ │                      │ │
│ │ }                │ │ │                      │ │
│ └──────────────────┘ │ └──────────────────────┘ │
├──────────────────────┴──────────────────────────┤
│ [▶ Send] [✓ Complete] [✗ Cancel]                │
│ ● IDLE | Sent: 0 | Received: 0                  │
└─────────────────────────────────────────────────┘
```

**关键变更**：
- 配置栏：更清晰的分组行
- Send/Complete/Cancel 按钮移至底部操作栏（不与配置混合）
- 底部状态栏统一

### 3.2 操作栏分离

**实现**：
- 从配置栏移除 Send/Complete/Cancel
- 在分割编辑器下方（状态栏上方）添加新的操作栏面板
- 操作栏同时包含流状态指示器

## 4. 共享模式

### 4.1 行内表格操作组件

创建可复用的 `InlineTableActionsHelper` 工具类：
- 为任意 `DefaultTableModel` 添加 "Actions" 列
- 提供每行显示删除按钮的渲染器/编辑器
- 在表格下方提供 "Add row" 链接
- 编辑最后一行时自动添加空行
- 使用方：三个 Tab 中的 Params、Headers、Metadata 表格

### 4.2 紧凑按钮样式

- 使用 `JBUI.Borders.empty()` 实现更紧凑的间距
- 辅助按钮使用较小字体
- 适当使用图标按钮（使用 IntelliJ 图标或 Unicode 符号）

## 需要修改的文件

| 文件 | 变更内容 |
|------|---------|
| `PostmanToolWindowPanel.kt` | 历史切换、行内表格操作、工具栏整合、动态尺寸 |
| `WebSocketPanel.kt` | 布局重构、Headers 作为 Tab、紧凑间距 |
| `GrpcEditorPanel.kt` | 操作栏分离、配置栏清理、状态栏 |
| `HistoryPanel.kt` | 小幅修改：确保动态显示/隐藏时正确工作 |
| 新建：`InlineTableActionsHelper.kt` | 可复用的行内表格操作列组件 |

## 非目标

- 不更改业务逻辑（请求发送、响应解析等）
- 不更改数据模型
- 不更改服务或工具类（UI 工具类除外）
- 除 UI 布局改进外不添加新功能
