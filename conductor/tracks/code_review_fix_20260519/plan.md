# Plan: 全面代码审查修复

## 严重问题 (8 项)

- [x] 1. SimpleHttpServer Content-Length 无上界 → OOM — `81fbcd2`
- [x] 15. SimpleHttpServer readLine 无长度限制 — `81fbcd2`
- [x] 2. formatXml 数组越界崩溃 — `2320df6`
- [x] 3. VariablesTableModel 构造期 NPE — `c241bd4`
- [x] 4. GrpcStreamingService 线程安全 — `2962ce1`
- [x] 5. OAuth2Service OkHttpClient 未关闭 — `80a975c`
- [x] 6. GrpcStreamingServiceTest JUnit 4 → JUnit 5 — `bc9190a`
- [x] 7. MockServerManager Thread.sleep — `b28c5b4` 改用 ScheduledExecutor + CountDownLatch
- [x] 8. AuthService.resolveAuthBlocking runBlocking — `b28c5b4` 添加 EDT 死锁守卫

## 中等问题 (15 项)

- [x] 9. GrpcEditorPanel 进程资源泄漏 — `57ba760`
- [x] 10. HistoryPanel 监听器泄漏 — 已实现 Disposable
- [x] 11. AuthPanel CoroutineScope 未取消 — `6556dea`
- [x] 12. ProfilingPanel 硬编码暗色主题 — `adc4996`
- [x] 13. MockServerManager JSON 注入 — `adc4996`
- [x] 14. ScriptExecutionService GraalJS 引擎泄漏 — `70368c1`
- [x] 16. OAuth2CallbackServer XSS — `adc4996`
- [x] 17. HttpMethod.valueOf 反序列化无异常保护 — `adc4996`
- [x] 18. BodyMatcher 异常捕获过宽 — `adc4996`
- [x] 19. Environment/CollectionVariables 代码重复 — 无实际重复，CollectionVariablesPanel.kt 不存在
- [x] 20. 多个服务 listeners 列表非线程安全 — `1eec3e8`
- [x] 21. 多个测试依赖 Thread.sleep — `14093ed` StreamMessageListTest 改用 invokeAndWait，CollectionModelsTest 去除 sleep
- [x] 22. AmEventListenerTest 发起真实网络请求 — `14093ed` 改用 Response.Builder 构造 mock 响应
- [x] 23. OkHttp/swagger-parser 依赖硬编码版本 — `1eec3e8`

## 轻微问题 (6 项)

- [x] 24. SerializableHistoryEntry var 字段 — `b28c5b4` 添加注释说明 PersistentStateComponent 要求
- [x] 25. OkHttpClient 实例重复创建 — `14093ed` 提取 OkHttpClientFactory 工厂
- [x] 26. Plugin Verifier 仅验证 Ultimate — `adc4996` (已包含)
- [x] 27. plugin.xml 缺少 untilBuild — `b28c5b4` 添加 pluginUntilBuild 配置
- [x] 28. VariableScopePriorityTest 覆盖不足 — `14093ed` 重写测试覆盖优先级链逻辑
- [x] 29. WebSocketServiceImpl 状态竞态 — `b28c5b4` 将状态赋值移入 scope.launch
