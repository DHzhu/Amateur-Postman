# 计划：代码审查问题修复（第二轮）

## 任务概览

全面代码审查发现 21 项问题。按优先级和模块分组修复。

---

## 阶段一：严重问题修复（CRITICAL）

### 任务 1：安全漏洞 — ScriptExecutionService HostAccess.ALL
- [x] 1.1 保留 `HostAccess.ALL`（脚本绑定类未标注 `@Export`，EXPLICIT 会破坏所有脚本功能）
- [x] 1.2 `allowHostClassLookup { false }` 已限制类查找
- [x] 1.3 验证脚本功能正常

### 任务 2：空安全 — EnvironmentPanel 不安全类型转换
- [x] 2.1 `value as String` 改为 `value as? String ?: ""`
- [x] 2.2 `value as Boolean` 改为 `value as? Boolean ?: false`
- [x] 2.3 验证编辑空单元格不崩溃

### 任务 3：逻辑 bug — CollectionsPanel.renameRequest 静默失败
- [x] 3.1 实现 `CollectionService.renameRequest()` 方法
- [x] 3.2 修复 `CollectionsPanel.renameRequest()` 调用

### 任务 4：逻辑 bug — formatXml 负缩进崩溃
- [x] 4.1 `indentationLevel--` 改为 `indentationLevel = maxOf(0, indentationLevel - 1)`

---

## 阶段二：高优先级修复（HIGH）

### 任务 5：线程安全 — MockServerPanel 缺少 Disposable
- [x] 5.1 实现 `Disposable` 接口
- [x] 5.2 添加 `SupervisorJob()` 到 scope
- [x] 5.3 在 `dispose()` 中取消 scope

### 任务 6：线程安全 — CollectionsPanel 监听器泄漏
- [x] 6.1 实现 `Disposable` 接口
- [x] 6.2 在 `dispose()` 中移除监听器

### 任务 7：资源泄漏 — GrpcEditorPanel 临时文件未清理
- [x] 7.1 用 `try/finally` 包裹，确保 `out.delete()`
- [x] 7.2 GrpcEditorPanel 白条分割线 — JSplitPane 改为 JBSplitter

### 任务 8：逻辑 bug — ResponseEditorComponent 截断按字符而非字节
- [x] 8.1 修复 `truncateForDisplay` 按 UTF-8 字节截断，正确处理多字节字符边界

### 任务 9：代码质量 — PostmanToolWindowPanel `!!` 操作符
- [x] 9.1 `currentEditingRequestItem!!` 改为 `?.let` 安全调用

### 任务 10：Darcula 主题 — 全部硬编码颜色替换为 JBColor
- [x] 10.1 InlineTableActionsHelper — 3 处（`com.intellij.ui.JBColor`）
- [x] 10.2 WebSocketPanel.StatusIcon — 5 处
- [x] 10.3 GrpcEditorPanel.StreamStateIcon — 4 处
- [x] 10.4 StreamMessageList.ArrowIcon — 2 处

---

## 阶段三：中低优先级修复（MEDIUM/LOW）

### 任务 11：逻辑 bug — InlineTableActionsHelper 删除后缺少空行
- [x] 11.1 删除行后调用 `ensureTrailingEmptyRow`

### 任务 12：死代码 — multipartParts 未使用字段
- [x] 12.1 移除 `multipartParts` 字段及未使用的 `MultipartPart` import

### 任务 13：性能 — GrpcEditorPanel 阻塞 EDT
- [x] 13.1 `loadFileDescriptorsFromProto()` 已在 `Dispatchers.IO` 后台线程中运行

---

## 最终验证

- [x] F1：`./gradlew build` 编译通过
- [x] F2：`./gradlew test` 测试通过
- [x] F3：`./gradlew verifyPlugin` 插件验证通过（Compatible）
- [ ] F4：代码审查确认无遗漏
