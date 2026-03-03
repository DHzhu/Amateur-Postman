# Implementation Plan: Mock Server 鲁棒性修复

## 阶段 1: 编写红 Phase 测试 (Red Phase) ✅
- [x] **步骤 1.1**: 编写针对大 Body 的测试用例 `MockServerOomTest.kt`。
- [x] **步骤 1.2**: 编写针对未知方法的测试用例，并断言返回 405。
- [x] **步骤 1.3**: 编写针对多规则优先级竞争的测试，并断言高优先级命中。

## 阶段 2: 核心代码加固 (Implementation) ✅
- [x] **步骤 2.1**: 修改 `MockRule` 模型，增加 `priority` 字段及其默认值。
- [x] **步骤 2.2**: 优化 `findMatchingRule` 的规则搜索顺序（按优先级降序）。
- [x] **步骤 2.3**: 重构 `MockHandler` 的请求体读取逻辑，增加长度限制。
- [x] **步骤 2.4**: 修正 `parseHttpMethod` 为严格匹配模式，并处理异常反馈。

## 阶段 3: 性能调优与异步处理 ✅
- [x] **步骤 3.1**: 修改 `handleMockResponse`，使用 CachedThreadPool 实现并发请求处理。
- [x] **步骤 3.2**: 添加线程池管理，支持延迟请求并发处理。

## 阶段 4: 最终验证与归档 ✅
- [x] **步骤 4.1**: 运行全量测试确认修复效果（288 tests passed）。
- [x] **步骤 4.2**: 提交修复代码并归档计划。

## 实现摘要

### 修复内容
1. **OOM 防护**: 请求体大小限制为 1MB，超出返回 413 Payload Too Large
2. **方法匹配**: 未知 HTTP 方法返回 405 Method Not Allowed（支持 GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS）
3. **规则优先级**: 新增 `priority` 字段，高优先级规则优先匹配
4. **并发优化**: 使用 CachedThreadPool 支持并发请求处理

### 测试覆盖
- `MockServerOomTest`: 6 tests - OOM 防护测试
- `MockServerMethodTest`: 8 tests - HTTP 方法测试
- `MockServerPriorityTest`: 8 tests - 规则优先级测试
