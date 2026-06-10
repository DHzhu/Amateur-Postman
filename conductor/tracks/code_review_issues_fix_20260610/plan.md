# Plan: 代码审查问题修复（第三轮）

## 1. 数值溢出崩溃修复 (1.1)
- [x] 1.1.1 修复 `VariableResolver.resolveRandomInt`：对 `max == Int.MAX_VALUE` 边界进行处理，避免 `max + 1` 溢出；同时对 `$randomInt:length` 格式的 `length` 参数增加上限防护（最大 9），防止 `10.0.pow(length).toInt()` 在 `length >= 10` 时溢出。
- [x] 1.1.2 修复 `ScriptExecutionService` 中 `PreRequestContext.randomInt` 引起的边界溢出（当 JS 调用 `am.randomInt(0, 2147483647)` 时 `max + 1` 溢出）。
- [x] 1.1.3 编写测试用例验证数值边界，覆盖：`max == Int.MAX_VALUE`、`length >= 10`、正常范围输入。

## 2. Mock 延迟非阻塞化 (1.2)
- [x] 2.2.1 重构 `MockServerManager.handleMockResponse`：用挂起协程机制或将写 Socket 逻辑调度到延迟执行器中，消除同步 `CountDownLatch.await()`。
- [x] 2.2.2 编写基准/并发测试，模拟高并发 Mock 请求，验证没有发生线程池暴增和阻塞。

## 3. JS 全局锁与网络 IO 瓶颈优化 (1.3)
- [x] 3.3.1 重构 `PmBinding.sendRequest`：将 `runBlocking` 网络请求卸载到独立线程池（`CompletableFuture`），避免在持有 `scriptExecutionMutex` 时阻塞 JS 引擎线程。
- [x] 3.3.2 评估 `scriptExecutionMutex` 的必要性：GraalVM Context 不是线程安全的，锁仍需保留，但已确保锁内不执行阻塞 I/O（网络请求已卸载到独立线程池）。
- [x] 3.3.3 编写并发测试，确保长耗时的 `pm.sendRequest` 脚本运行时，其他并发脚本能并发执行。

## 4. OAuth2 回调 CSRF 校验防御 (1.4)
- [x] 4.4.1 在 `OAuth2Service.startAuthorizationCodeFlow` 中，将生成的 `state` UUID 与 `configId` 关联暂存在 `pendingAuthStates` ConcurrentHashMap 中。
- [x] 4.4.2 在 `OAuth2Service.waitForAuthCodeAndExchange` 接收到回调时，取出回调的 `state`，与内存中暂存的 `state` 进行一致性核对。仅当匹配时才允许后续 Token 交换；不匹配时返回 CSRF 错误。
- [x] 4.4.3 补充 `OAuth2Service` 回调安全测试：覆盖 state 存储、state UUID 格式校验。

## 5. PersistentStateComponent 竞态写入修复 (1.5)
- [x] 5.5.1 对 `EnvironmentService` 中的所有 `state = state.copy(...)` 操作引入 `synchronized(stateLock)` 互斥同步。已覆盖方法：
  - `createEnvironment()`、`deleteEnvironment()`、`updateEnvironment()`、`renameEnvironment()`
  - `setCurrentEnvironment()`、`clearCurrentEnvironment()`
  - `setGlobalVariable()`、`removeGlobalVariable()`
  - `setCollectionVariable()`、`removeCollectionVariable()`
- [x] 5.5.2 对 `OAuth2Service` 中的所有 `state = state.copy(...)` 操作引入 `synchronized(stateLock)` 互斥同步。已覆盖方法：
  - `createConfig()`、`updateConfig()`、`renameConfig()`、`deleteConfig()`
  - `setToken()`、`clearToken()`
  - `setRequestAuthConfig()`、`setCollectionAuthConfig()`
- [x] 5.5.3 编写并发读写单元测试，验证不会有配置或 Token 数据被覆盖丢失。

## 6. 归档与总结 (1.6)
- [x] 6.6.1 运行 `./gradlew clean test` 全量测试通过（670+ tests, 0 failures）。
- [x] 6.6.2 整理 CHANGELOG 并做归档处理。
