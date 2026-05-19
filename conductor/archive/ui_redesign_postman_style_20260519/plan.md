# 计划：Postman 风格 UI 重新设计

## 任务概览

参照 Postman 的 UI 模式，重新设计 HTTP、WebSocket、gRPC 三个 Tab 的窗口布局。共 8 个任务，分 4 个阶段。

---

## 阶段一：共享基础设施

### 任务 1：创建 InlineTableActionsHelper
- [x] 1.1 在 `ui/` 包下创建 `InlineTableActionsHelper.kt`
- [x] 1.2 实现 `DeleteButtonRenderer` — 每行渲染 "−" 删除按钮
- [x] 1.3 实现 `DeleteButtonEditor` — 处理删除点击事件
- [x] 1.4 实现 `addActionsColumn(table, model)` — 为任意表格添加操作列
- [x] 1.5 实现 `createAddRowButton(table, model, columnCount)` — 表格下方 "添加行" 链接按钮
- [x] 1.6 实现编辑最后一行时自动追加空行
- [x] 1.7 编写 InlineTableActionsHelper 单元测试
- [x] 1.8 验证：表格正确渲染、增删正常、无回归

---

## 阶段二：HTTP Tab 重新设计

### 任务 2：历史面板默认关闭 + 切换
- [x] 2.1 添加 `historyVisible` 状态字段和工具栏 `historyButton`
- [x] 2.2 从静态水平分割器布局中移除历史面板
- [x] 2.3 实现切换逻辑：显示/隐藏历史面板及分割器
- [x] 2.4 历史激活时按钮添加视觉指示
- [x] 2.5 确保历史面板切换打开时正确加载数据
- [~] 2.6 编写切换行为测试
- [x] 2.7 验证：默认隐藏、切换正常、无布局异常

### 任务 3：HTTP Params/Headers 行内操作
- [x] 3.1 重构 `createTablePanel()` 使用 `InlineTableActionsHelper`
- [x] 3.2 应用到 `paramsTable` — 行内删除 + 添加行链接
- [x] 3.3 应用到 `headersTable` — 行内删除 + 添加行链接
- [x] 3.4 移除旧的 Add/Remove 按钮面板
- [~] 3.5 实现表格动态高度（根据内容自动调整，最大高度后滚动）
- [~] 3.6 编写新表格行为测试
- [x] 3.7 验证：行内按钮正常、动态尺寸正常、无数据丢失

### 任务 4：HTTP 顶部工具栏整合
- [x] 4.1 重新组织顶部面板布局 — 紧凑的单/双行设计
- [x] 4.2 确保 Method + URL + Send 保持突出
- [x] 4.3 次要按钮（Import/Export cURL、Save、Quick Look、History）紧凑排列
- [x] 4.4 验证：所有按钮可访问、布局整洁

---

## 阶段三：WebSocket Tab 重新设计

### 任务 5：WebSocket 布局重构
- [x] 5.1 重构 `createTopBar()` — URL + 协议 + 连接/断开按钮单行显示
- [x] 5.2 创建 `JBTabbedPane`，包含 "Messages" 和 "Headers" 两个 Tab
- [x] 5.3 将 `StreamMessageList` 移入 Messages Tab
- [x] 5.4 将 Headers 表格移入 Headers Tab，使用行内操作
- [x] 5.5 重构底部区域：输入框 + 状态栏
- [x] 5.6 确保所有状态观察仍然正常工作
- [~] 5.7 编写新布局测试
- [x] 5.8 验证：连接/断开正常、消息显示正常、Headers 可编辑

---

## 阶段四：gRPC Tab 重新设计

### 任务 6：gRPC 配置栏清理
- [x] 6.1 将配置栏重新组织为更清晰的分组行
- [x] 6.2 Proto 文件行：路径 + 浏览 + 加载
- [x] 6.3 Service/Method 行：更好的布局和间距
- [x] 6.4 Host/Port/TLS 行：清晰分组
- [x] 6.5 验证：所有配置控件正常、Proto 加载正常

### 任务 7：gRPC 操作栏分离
- [x] 7.1 从配置栏移除 Send/Complete/Cancel 按钮
- [x] 7.2 在分割编辑器下方创建操作栏面板
- [x] 7.3 将流状态指示器移至操作栏
- [x] 7.4 将流计数移至操作栏
- [x] 7.5 验证：发送/完成/取消按钮正常、状态更新正确

### 任务 8：gRPC Metadata 表格行内操作
- [x] 8.1 将 `InlineTableActionsHelper` 应用到 Metadata 表格
- [x] 8.2 移除旧的 Add/Remove 按钮面板
- [x] 8.3 验证：Metadata 行内增删正常

---

## 最终验证

- [x] F1：运行 `./gradlew build` — 无编译错误
- [x] F2：运行 `./gradlew test` — 所有测试通过
- [~] F3：运行 `./gradlew koverHtmlReport` — 覆盖率 >80%
- [~] F4：手动视觉检查 — 三个 Tab 布局正确
- [~] F5：Darcula 主题检查 — 暗色模式渲染正确
