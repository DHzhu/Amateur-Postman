# Plan: 全面代码审查修复

## 严重问题 (8 项)

- [x] 1. SimpleHttpServer Content-Length 无上界 → OOM — `81fbcd2`
- [x] 15. SimpleHttpServer readLine 无长度限制 — `81fbcd2`
- [x] 2. formatXml 数组越界崩溃 — `2320df6`
- [x] 3. VariablesTableModel 构造期 NPE — `c241bd4`
- [x] 4. GrpcStreamingService 线程安全 — `2962ce1`
- [x] 5. OAuth2Service OkHttpClient 未关闭 — `80a975c`
- [x] 6. GrpcStreamingServiceTest JUnit 4 → JUnit 5 — `bc9190a`
- [~] 7. MockServerManager Thread.sleep — 设计选择，延迟特性需要，暂不修改
- [~] 8. AuthService.resolveAuthBlocking runBlocking — IntelliJ 插件常见模式，风险低，暂不修改

## 中等问题 (15 项)

- [x] 9. GrpcEditorPanel 进程资源泄漏 — `57ba760`
- [~] 10. HistoryPanel 监听器泄漏 — 死代码，未被实例化，跳过
- [x] 11. AuthPanel CoroutineScope 未取消 — `6556dea`
- [x] 12. ProfilingPanel 硬编码暗色主题 — `adc4996`
- [x] 13. MockServerManager JSON 注入 — `adc4996`
- [x] 14. ScriptExecutionService GraalJS 引擎泄漏 — `70368c1`
- [x] 16. OAuth2CallbackServer XSS — `adc4996`
- [x] 17. HttpMethod.valueOf 反序列化无异常保护 — `adc4996`
- [x] 18. BodyMatcher 异常捕获过宽 — `adc4996`
- [~] 19. Environment/CollectionVariables 代码重复 — 重构，暂不修改
- [x] 20. 多个服务 listeners 列表非线程安全 — `1eec3e8`
- [~] 21. 多个测试依赖 Thread.sleep — 测试重构，暂不修改
- [~] 22. AmEventListenerTest 发起真实网络请求 — 测试重构，暂不修改
- [x] 23. OkHttp/swagger-parser 依赖硬编码版本 — `1eec3e8`

## 轻微问题 (6 项)

- [~] 24. SerializableHistoryEntry var 字段 — PersistentStateComponent 要求，可接受
- [~] 25. OkHttpClient 实例重复创建 — 架构优化，暂不修改
- [x] 26. Plugin Verifier 仅验证 Ultimate — `adc4996` (已包含)
- [~] 27. plugin.xml 缺少 untilBuild — 需评估兼容性策略
- [~] 28. VariableScopePriorityTest 覆盖不足 — 测试重构，暂不修改
- [~] 29. WebSocketServiceImpl 状态竞态 — 影响低，暂不修改
