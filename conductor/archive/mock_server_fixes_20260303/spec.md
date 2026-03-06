# Specification: Mock Server 鲁棒性修复

## 目标
根据 2026-03-03 审计结论，修复 `MockServerManager.kt` 中的四大核心风险。

## 修复项定义

### 1. 防御性资源读取 (OOM 保护)
- **方案**: 引入 `MAX_MOCK_BODY_SIZE = 1 * 1024 * 1024` (1MB) 限制。
- **行为**: 使用流式读取请求体，若超过 1MB 则中断读取并向客户端返回 `413 Payload Too Large`。

### 2. 严格 HTTP 方法解析
- **方案**: 修正 `parseHttpMethod`，对于未知方法抛出 `UnknownMethodException`。
- **行为**: 在 `handle` 函数中捕获该异常，并返回 `405 Method Not Allowed`，不再降级为 `GET`。

### 3. 确定性规则匹配 (优先级机制)
- **方案**: 在 `MockRule` 模型中新增 `priority: Int` 字段 (默认值 0)。
- **行为**: `findMatchingRule` 在匹配前，先执行 `rules.values.sortedByDescending { it.priority }`。

### 4. 非阻塞延迟处理
- **方案**: 评估是否能使用协程 `delay` 或 `ScheduledExecutorService` 异步响应。
- **行为**: 最小化 `Thread.sleep` 对 `HttpServer` 工作线程池的占用。

## 验收标准
- 单元测试通过，模拟大包体触发 413。
- 单元测试通过，模拟未知方法触发 405。
- 单元测试通过，模拟多规则共存时高优先级优先匹配。
- 压力测试通过，多条长延迟请求不应导致 Mock Server 彻底卡死。
