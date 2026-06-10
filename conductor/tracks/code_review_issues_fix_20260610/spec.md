# Spec: 代码审查问题修复（第三轮）

## 背景
通过对项目核心代码库的质量与安全审查，发现 5 项关键的技术缺陷，涉及数值计算边界、伪异步线程阻塞、全局执行锁并发瓶颈、OAuth2 流程中的 CSRF 验证缺失，以及配置持久化服务中的线程安全写入问题。

## 修复范围与设计方案

### 1. `Random.nextInt` 数值边界溢出防护
*   **关联代码**: 
    *   [VariableResolver.kt:L186](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/utils/VariableResolver.kt#L186)
    *   [VariableResolver.kt:L201](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/utils/VariableResolver.kt#L201)
    *   [ScriptExecutionService.kt:L114](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/ScriptExecutionService.kt#L114)
*   **设计方案**:
    *   对传入的 `max` 边界值增加防护。若 `max == Int.MAX_VALUE`，直接进行安全范围生成（例如采用 `Random.nextInt(min, Int.MAX_VALUE)`，然后再结合判断是否需要补充包含 `Int.MAX_VALUE` 边界，或者扩大至 `Long` 范围生成后转换），避免 `max + 1` 溢出。
    *   在 `$randomInt:length` 解析处，对 length 进行范围限制（如最大不超过 9），以防止超出 `Int.MAX_VALUE`。

### 2. `MockServerManager` 的异步非阻塞延时改造
*   **关联代码**: 
    *   [MockServerManager.kt:L305-L310](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/MockServerManager.kt#L305-L310)
*   **设计方案**:
    *   消除同步阻塞的 `CountDownLatch.await()`。
    *   将 Mock 响应的构建与发送逻辑全部移动至 `delayExecutor` 的调度回调任务中去异步处理，使处理 HTTP 请求的工作线程能在延迟期间释放，提高 Mock 服务的并发吞吐率。

### 3. `ScriptExecutionService` 并发性能优化
*   **关联代码**: 
    *   [ScriptExecutionService.kt:L747](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/ScriptExecutionService.kt#L747)，[L802](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/ScriptExecutionService.kt#L802) 与 [PmBinding.sendRequest:L332](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/ScriptExecutionService.kt#L332)
*   **设计方案**:
    *   重构 `PmBinding.sendRequest`：将 `runBlocking { svc.executeRequest(req) }` 网络请求卸载到独立的 `requestExecutor` 线程池（通过 `CompletableFuture`），避免在持有 `scriptExecutionMutex` 全局锁时阻塞 JS 引擎线程。
    *   保留 `scriptExecutionMutex`：GraalVM Context 非线程安全，锁仍需保留以序列化 JS 引擎访问，但锁内不再执行阻塞 I/O。

### 4. OAuth2 回调 `state` 安全校验
*   **关联代码**: 
    *   [OAuth2Service.kt:L525](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/OAuth2Service.kt#L525)
    *   [OAuth2CallbackHandler.kt:L112](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/OAuth2CallbackHandler.kt#L112)
*   **设计方案**:
    *   在 `OAuth2Service` 开始 Authorization Code 重定向流程前，将当前生成的 `state` UUID 关联 `configId` 暂存在内存中。
    *   在 `OAuth2CallbackServer` 接收到回调时，取出 URL 里的 `state`，并与内存中的 `state` 进行一致性核对。仅当两者相符时，才允许执行后续的 authorization code 交换 Token 操作，以防止 CSRF。

### 5. `PersistentStateComponent` 的线程安全写入
*   **关联代码**: 
    *   [OAuth2Service.kt](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/OAuth2Service.kt) 和 [EnvironmentService.kt](file:///projects/Amateur-Postman/src/main/kotlin/com/github/dhzhu/amateurpostman/services/EnvironmentService.kt)
*   **设计方案**:
    *   引入线程同步控制（例如使用 Kotlin 协程 `Mutex`），为所有对 `state` 写入和修改的操作进行加锁，保证对 `state = state.copy(...)` 赋值过程的原子性与串行化。

## 约束
*   不引入任何第三方依赖。
*   不改动对外暴露的 API 结构。
*   确保修复后能 100% 通过编译与测试套件。
