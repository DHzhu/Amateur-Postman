# Amateur-Postman Changelog

所有对本项目的显著变更都将记录在此文件中。

本项目遵循 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) 规范。

## [Unreleased]

## [0.0.7] - 2026-06-09
### Fixed
- **Collections 重复展示**: 移除 tabbedPane 中的 Collections tab，仅保留工具栏 toggle 按钮，修复 Collections 同时出现在两个位置的问题。
- **Collections 持久化失效**: `SerializableCollection`、`SerializableCollectionItem`、`SerializableHttpRequest`、`SerializableAuthentication` 等序列化模型补全无参构造器默认值，修复 IntelliJ `XmlSerializer` 反序列化静默失败导致重启后数据丢失。
- **Environments 持久化失效**: `SerializableEnvironment`、`SerializableVariable`、`SerializableCollectionVariables` 补全默认值，修复环境变量重启后丢失。
- **OAuth2 持久化失效**: `OAuth2Config`、`OAuth2Token`、`OAuth2ConfigEntry`、`RequestAuthMapping`、`CollectionAuthMapping` 补全默认值，修复 OAuth2 配置重启后丢失。
- **协程取消传播**: `HttpRequestServiceImpl`、`OAuth2Service`、`ScriptExecutionService` 中 `catch (e: Exception)` 吞掉 `CancellationException`，导致协程取消无法正确传播。
- **PersistentStateComponent 线程安全**: `MockServerManager`、`RequestHistoryService`、`OAuth2Service` 的 `state` 字段补充 `@Volatile` 注解。
- **资源泄漏**: `MockServerManager.dispose()` 未停止 HTTP Server、`WebSocketServiceImpl.dispose()` 未清理 ConnectionPool、`PostmanToolWindowPanel.dispose()` 未清理子组件 Disposable、`EnvironmentPanel` 未实现 Disposable、`CollectionRunnerDialog` CoroutineScope 未取消。
- **WebSocket 线程安全**: `WebSocketServiceImpl.webSocket` 字段补充 `@Volatile` 注解，移除未使用的 `stateMutex`。
- **gRPC 线程安全**: `GrpcStreamingService.requestObserver` 和 `currentChannel` 字段补充 `@Volatile` 注解。
- **并发安全**: `MockServerManager.getState()` 改用 copy-on-write 模式，`RequestHistoryService` 的 `addEntry`/`deleteEntry`/`clearHistory`/`renameEntry` 改用不可变列表。

## [0.0.6] - 2026-06-08
### Added
- **Collections 工具栏按钮**: 在工具栏 History 按钮前新增 Collections 入口，点击可展开/收起 Collections 侧边栏。
- **Params 显式按钮**: Params 表格新增 Add/Remove 按钮，作为内联 +/- 按钮的备用操作方式。
- **保存更新模式**: 从 Collections 加载的请求再次保存时，弹出 Update / Save as New / Cancel 选项，避免重复保存。

### Fixed
- **IntelliJ 2025.3 兼容性**: `InlineTableActionsHelper` 单元格编辑器改用 `currentRow` 替代 `table.editingRow`，并在操作前调用 `table.removeEditor()` 确保编辑器完全释放，修复 2025.3 上 Params/Headers 添加删除按钮失效的问题。
- **变量解析**: 发送请求时先解析环境变量再验证 URL 格式，支持 `{{variable}}` 语法；未解析的变量给出明确错误提示。
- **环境切换**: `EnvironmentWrapper` 实现 `equals()`/`hashCode()`，修复创建新环境后无法通过 ComboBox 切换的问题。
- **环境加载防抖**: `loadEnvironments` 期间设置 `isLoadingEnvironments` 标志，防止 ComboBox 重绘时触发多余的环境切换事件。
- **变量编辑自动保存**: 环境变量和全局变量的表格编辑操作实时回写到 `EnvironmentService`，不再需要额外保存步骤。
- **Save 对话框**: 重写为 `GridBagLayout` 布局，修复空白窗口问题；使用 IntelliJ 标准 `doOKAction` 处理确认逻辑。
- **响应换行符**: `ResponseEditorComponent.setContent` 将 `\r\n` 规范化为 `\n`，修复 HTTP 响应含 Windows 换行符时抛出 `Wrong line separators` 异常。
- **UI 文案**: 错误提示和脚本说明统一改为英文。

## [0.0.5] - 2026-05-19
### Fixed
- **安全漏洞**: SimpleHttpServer 增加 Content-Length 上界（1MB）和 Header 行长度限制（8KB），防止 OOM 攻击。
- **安全漏洞**: OAuth2CallbackServer 修复 XSS 漏洞，对错误信息进行 HTML 转义。
- **安全漏洞**: MockServerManager 修复 JSON 注入风险，使用 Jackson 序列化替代字符串拼接。
- **资源泄漏**: GrpcEditorPanel protoc 进程超时/异常时正确销毁。
- **资源泄漏**: OAuth2Service、AuthPanel、ScriptExecutionService 实现 `Disposable` 接口，正确释放 OkHttpClient、CoroutineScope、GraalJS 引擎。
- **线程安全**: GrpcStreamingService 共享状态改用 `AtomicInteger` + `synchronized`。
- **线程安全**: 多个服务的 listeners 列表改用 `CopyOnWriteArrayList`。
- **线程安全**: WebSocketServiceImpl `onFailure` 回调中状态赋值移入 `scope.launch` 消除竞态。
- **崩溃修复**: `VariablesTableModel` 从 `DefaultTableModel` 改为 `AbstractTableModel`，修复构造期 NPE。
- **崩溃修复**: `formatXml` 修复 `'<'` 后字符越界和运算符优先级问题。
- **崩溃修复**: `HttpMethod.valueOf` 反序列化增加异常保护，避免 `IllegalArgumentException`。
- **崩溃修复**: `BodyMatcher` 异常捕获从 `Exception` 收窄为 `IllegalArgumentException`。
- **死锁防护**: `AuthService.resolveAuthBlocking` 增加 EDT 线程检查，防止死锁。
- **UI**: `ProfilingPanel` 硬编码暗色主题颜色改为 `JBColor` 主题感知。
- **Mock Server**: 延迟规则从 `Thread.sleep` 改用 `ScheduledExecutor` + `CountDownLatch`，避免阻塞 HTTP 线程池。
- **构建**: `pluginUntilBuild` 配置为 `253.*`，明确兼容性范围。
- **依赖**: OkHttp 和 swagger-parser 版本纳入 `libs.versions.toml` 统一管理。

### Changed
- **测试质量**: `AmEventListenerTest` 消除真实网络请求，改用 `Response.Builder` 构造 mock 响应。
- **测试质量**: `StreamMessageListTest` 去除 9 处 `Thread.sleep`，改用 `invokeAndWait` 确定性同步。
- **测试质量**: `VariableScopePriorityTest` 从空壳测试扩展为覆盖完整优先级链。
- **基础设施**: 提取 `OkHttpClientFactory` 统一生产 OkHttpClient 配置。

## [0.0.4] - 2026-05-18
### Fixed
- **Internal API 兼容性**: 移除 `com.sun.net.httpserver`、`EditorEx`、`XmlSerializerUtil` 等 Internal API 使用，通过 JetBrains Marketplace Plugin Verifier 验证。
- **SimpleHttpServer**: 新建基于 `java.net.ServerSocket` 的轻量 HTTP 服务器，替代 `com.sun.net.httpserver`。
- **ResponseEditorComponent**: 使用 `EditorFactory.createEditor()` 公共 API 替代 `EditorEx` 内部接口。
- **Kotlin 版本对齐**: Kotlin 从 2.3.0 降至 2.2.0，匹配 IntelliJ Platform 2025.1.1 内置版本，修复 coroutines debug metadata 版本不匹配问题。

## [0.4.3] - 2026-03-18
### Changed
- **响应查看器升级**: 将 `HighPerfResponseViewer` 从 `JTextPane` 迁移至 IntelliJ 原生 `EditorFactory.createViewer`，获得虚拟滚动、内置折叠、内置 Ctrl+F 搜索及主题自动适配能力。
- **语法高亮优化**: 基于响应 `Content-Type` 自动选择文件类型驱动原生语法高亮（JSON/XML/HTML/纯文本），JSON 响应自动美化输出。

### Added
- **ResponseEditorComponent**: 新增 IntelliJ Editor 封装组件，实现 `Disposable` 生命周期管理，确保编辑器实例正确释放。
- **大响应截断**: 超过 10MB 的响应体自动截断显示，并在顶部提示原始大小。
- **Save to File 按钮**: 响应区新增「Save」按钮，可将响应体保存为本地文件（自动推断扩展名）。

## [0.4.2] - 2026-03-18
### Changed
- **JSON 基础设施迁移**: 彻底移除 `com.google.gson:gson` 依赖，全线迁移至 `jackson-databind 2.18.2` + `jackson-module-kotlin`，统一由 `JsonService` 单例管理 `ObjectMapper`。
- **Kotlin 类型安全**: Jackson 反序列化现严格遵守 Kotlin 非空约束与默认参数，不再绕过构造函数（Gson 的 `Unsafe` 行为已消除）。
- **JsonToSchemaConverter**: 将 Gson `JsonElement` 树 API 替换为 Jackson `JsonNode`，保持功能等价。
- **ScriptExecutionService**: `PmResponseBinding.json()` 及 `sendRequest` 序列化均迁移至 Jackson；JS 侧 `pm.response.json()` 依然使用 `JSON.parse` 确保原生 JS 类型。

### Added
- **JsonService**: 新增应用级 JSON 服务单例，提供 `mapper`（美化输出）与 `compactMapper`（紧凑）两个预配置 `ObjectMapper` 实例。

## [0.4.1] - 2026-03-17
### Changed
- **OpenAPI 导出器重构**: 弃用自定义 POJO 拼装，改用 `io.swagger.v3.oas.models` 官方模型库，序列化由 `swagger-core` 的 `Yaml/Json.pretty()` 接管，与导入器保持技术一致性。

### Added
- **Request Headers 导出**: 导出操作现支持将 HTTP 请求头映射为 OpenAPI `HeaderParameter`，完整保留自定义业务头信息。
- **敏感信息过滤**: 导出时默认过滤 `Authorization`、`Cookie`、`Set-Cookie` 等敏感头字段，防止凭证泄露到公开 API 文档。
- **"Include sensitive headers" 开关**: 导出对话框新增勾选项，用户可按需决定是否包含敏感请求头。
- **响应历史推断**: `exportCollection` 支持传入 `RequestHistoryService`，自动从历史记录中查找匹配请求的最近成功响应，使用真实状态码（而非硬编码 "200 OK"）。
- **JsonToSchemaConverter**: 新增简易 JSON → OpenAPI Schema 转换工具，支持对象、数组、原始类型的结构推断。

## [0.4.0] - 2026-03-17
### Added
- **API 导出支持 (OpenAPI)**: 支持将集合导出为 OpenAPI 3.0.3 规范（YAML/JSON），自动映射文件夹层级为 Tags。
- **HAR 导入支持**: 支持导入 .har 文件，具备静态资源过滤、Host 分组及选择性导入预览界面。
- **统一认证框架**: 引入 Authentication 继承模型，全面支持 OAuth 2.0 (Authorization Code, Client Credentials, etc.) 流程及令牌自动刷新。
- **OpenAPI 深度集成**: 支持 OpenAPI 规范的导入、实时同步以及 IDE 代码与请求的深度联动。
- **协议扩展 (流式)**: 支持 WebSocket 消息收发及 gRPC (Server/Client/Bi-Di) 流式调用，提供高性能消息列表展示。
- **协议扩展 (gRPC)**: 引入 gRPC 基础支持，支持解析 .proto 文件并进行 Unary 调用。
- **变量可视化 (Quick Look)**: 新增 Environment Quick Look 悬浮窗，支持变量来源追踪与优先级高亮。
- **UI/UX 性能优化**: 引入虚拟滚动技术支持大规模集合展示，优化 High-Perf Response Viewer 以处理 10MB+ 响应。

## [0.3.0] - 2026-03-03
### Added
- **脚本引擎增强**: 集成 GraalVM JS，全面支持 `pm.sendRequest` (异步)、`chai.js` 断言及 `CryptoJS`。
- **Mock Server 强化**: 支持规则优先级匹配、HTTP 方法过滤、大报文 OOM 保护及多端口管理。
- **测试覆盖**: 新增 120+ 脚本执行测试与 288+ Mock Server 稳定性测试。
### Changed
- **测试框架迁移**: 完成从 JUnit 4 到 **JUnit 5** 的全量迁移，覆盖 266 个验证点。

## [0.2.0] - 2026-02-25
### Added
- **多级变量系统**: 实现 Global, Environment, Collection 三层作用域解析。
- **请求历史管理**: 支持按日期归档的历史记录持久化。
- **GraphQL 支持**: 初步支持 GraphQL 请求体预览与发送。

## [0.1.0] - 2026-02-15
### Added
- **核心能力**: 支持基础 GET/POST/PUT/DELETE 请求发送。
- **集合导出/导入**: 支持 Postman 格式 (v2.1) 的导入与基本导出。
- **基础 UI**: 基于 IntelliJ ToolWindow 实现的极简测试面板。

---
[0.0.6]: https://github.com/DHzhu/Amateur-Postman/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/DHzhu/Amateur-Postman/compare/v0.0.4...v0.0.5
[0.4.0]: https://github.com/DHzhu/Amateur-Postman/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/DHzhu/Amateur-Postman/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/DHzhu/Amateur-Postman/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/DHzhu/Amateur-Postman/releases/tag/v0.1.0
