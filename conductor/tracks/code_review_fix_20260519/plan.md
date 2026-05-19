# Plan: 全面代码审查修复

## 严重问题 (8 项)

- [~] 1. SimpleHttpServer Content-Length 无上界 → OOM — 已修复，待测试验证
- [~] 15. SimpleHttpServer readLine 无长度限制 — 已修复，待测试验证
- [ ] 2. formatXml 数组越界崩溃 (PostmanToolWindowPanel.kt:487,491)
- [ ] 3. VariablesTableModel.setValueAt 类型转换崩溃 (EnvironmentPanel.kt:487-489)
- [ ] 4. GrpcStreamingService 线程安全 (GrpcStreamingService.kt:55-61)
- [ ] 5. OAuth2Service OkHttpClient 从未关闭 (OAuth2Service.kt:44-50)
- [ ] 6. GrpcStreamingServiceTest 混用 JUnit 4 API (GrpcStreamingServiceTest.kt:3-4)
- [ ] 7. MockServerManager Thread.sleep 阻塞线程池 (MockServerManager.kt:287)
- [ ] 8. AuthService.resolveAuthBlocking runBlocking 死锁风险 (AuthService.kt:168)

## 中等问题 (15 项)

- [ ] 9. GrpcEditorPanel 进程资源泄漏 (GrpcEditorPanel.kt:783)
- [ ] 10. HistoryPanel 监听器泄漏 (HistoryPanel.kt:50)
- [ ] 11. AuthPanel CoroutineScope 从未取消 (AuthPanel.kt:47)
- [ ] 12. ProfilingPanel 硬编码暗色主题颜色 (ProfilingPanel.kt:100)
- [ ] 13. MockServerManager JSON 注入 (MockServerManager.kt:308)
- [ ] 14. ScriptExecutionService GraalJS 引擎泄漏 (ScriptExecutionService.kt:402)
- [ ] 16. OAuth2CallbackServer XSS (OAuth2CallbackHandler.kt:141)
- [ ] 17. HttpMethod.valueOf 反序列化无异常保护 (多文件)
- [ ] 18. BodyMatcher 异常捕获过宽 (MockModels.kt:38)
- [ ] 19. Environment/CollectionVariables 代码重复 (EnvironmentModels.kt)
- [ ] 20. 多个服务 listeners 列表非线程安全 (4 个 Service 文件)
- [ ] 21. 多个测试依赖 Thread.sleep flaky (多测试文件)
- [ ] 22. AmEventListenerTest 发起真实网络请求 (AmEventListenerTest.kt:222)
- [ ] 23. OkHttp/swagger-parser 依赖硬编码版本 (build.gradle.kts:44,66)

## 轻微问题 (6 项)

- [ ] 24. SerializableHistoryEntry 使用 var 字段 (RequestHistoryEntry.kt:71)
- [ ] 25. OkHttpClient 实例重复创建 (OAuth2Service + HttpRequestServiceImpl)
- [ ] 26. Plugin Verifier 仅验证 Ultimate 版本 (build.gradle.kts:144)
- [ ] 27. plugin.xml 缺少 untilBuild 配置 (build.gradle.kts:125)
- [ ] 28. VariableScopePriorityTest 覆盖不足 (VariableScopePriorityTest.kt)
- [ ] 29. WebSocketServiceImpl 状态竞态 (WebSocketServiceImpl.kt:110)
