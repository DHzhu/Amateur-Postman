# Implementation Plan: Mock Server 鲁棒性修复

## 阶段 1: 编写红 Phase 测试 (Red Phase)
- [x] **步骤 1.1**: 编写针对大 Body 的测试用例 `MockServerOomTest.kt`。 (e26eaa7)
- [x] **步骤 1.2**: 编写针对未知方法的测试用例，并断言返回 405。 (e26eaa7)
- [x] **步骤 1.3**: 编写针对多规则优先级竞争的测试，并断言高优先级命中。 (e26eaa7)

## 阶段 2: 核心代码加固 (Implementation)
- [x] **步骤 2.1**: 修改 `MockRule` 模型，增加 `priority` 字段及其默认值。 (e26eaa7)
- [x] **步骤 2.2**: 优化 `findMatchingRule` 的规则搜索顺序（按优先级降序）。 (e26eaa7)
- [x] **步骤 2.3**: 重构 `MockHandler` 的请求体读取逻辑，增加长度限制。 (e26eaa7)
- [x] **步骤 2.4**: 修正 `parseHttpMethod` 为严格匹配模式，并处理异常反馈。 (e26eaa7)

## 阶段 3: 性能调优与异步处理
- [x] **步骤 3.1**: 修改 `handleMockResponse`，评估协程或 `ScheduledExecutor` 实现。 (e26eaa7)
- [x] **步骤 3.2**: 移除 `Thread.sleep` 阻塞调用。 (e26eaa7)

## 阶段 4: 最终验证与归档
- [x] **步骤 4.1**: 运行全量测试确认修复效果。 (e26eaa7)
- [x] **步骤 4.2**: 提交修复代码并归档计划。 (e26eaa7)
