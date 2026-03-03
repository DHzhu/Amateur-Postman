# Implementation Plan: Scripting Power-up

## 阶段 1: 基础结构与库集成
- [ ] **步骤 1.1**: 下载并集成 `chai.js` 到 `src/main/resources/js/`。 (1h)
- [ ] **步骤 1.2**: 修改 `ScriptExecutionService.kt`，在引擎启动时自动加载 `chai.js`。 (0.5h)
- [ ] **步骤 1.3**: 将 `chai.expect` 绑定到 `pm.expect`。 (0.5h)
- [ ] **步骤 1.4**: 验证测试脚本中使用 `pm.expect(true).to.be.true` 正常运行。 (0.5h)

## 阶段 2: `pm.sendRequest` 核心实现 (关键挑战)
- [ ] **步骤 2.1**: 定义 `AmSendRequestBinding` 类，包含对 `HttpRequestService` 的依赖。 (1h)
- [ ] **步骤 2.2**: 实现同步化的异步请求：由于 `eval()` 是同步的，需在 `sendRequest` 中开启协程并行读取。 (2h)
- [ ] **步骤 2.3**: 编写单元测试 `ScriptExecutionServiceEnhancedTest.kt`，验证脚本内发起二次请求。 (1h)
- [ ] **步骤 2.4**: 优化 `callback(err, response)` 参数映射。 (1h)

## 阶段 3: 响应解析与 Proxy 增强
- [ ] **步骤 3.1**: 将 `pm.response.json()` 返回的结果通过 JS 封装为 Proxy，使其能通过 `.` 直接访问属性。 (1.5h)
- [ ] **步骤 3.2**: 增加常用断言快捷方式 `pm.response.to.have.status` 等。 (1h)

## 阶段 4: 进阶库集成与最终验证
- [ ] **步骤 4.1**: 集成 `ajv` 实现 JSON Schema 校验。 (1h)
- [ ] **步骤 4.2**: 运行全量测试验证脚本库加载对性能的影响。 (1h)
- [ ] **步骤 4.3**: 最终归档。 (0.5h)
