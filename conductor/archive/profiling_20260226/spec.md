# Specification - Phase 5: 性能分析 (Profiling)

## Overview
为 Amateur-Postman 引入性能分析功能，帮助开发者深入了解 HTTP 请求的各个阶段（如 DNS 查询、TCP 连接、SSL 握手等）的耗时情况。

## Functional Requirements
1. **Metric Collection**:
   - 利用 OkHttp 的 `EventListener` 拦截并记录以下网络阶段的时间戳：
     - DNS Lookup
     - TCP Connection (Connect Start -> Connect End)
     - SSL Handshake (if HTTPS)
     - Time to First Byte (TTFB)
     - Total Response Time
2. **UI Presentation**:
   - 在响应区域（Response Area）新增一个名为 "Profiling" 的标签页。
   - 使用列表或简单的可视化条形图展示各阶段耗时。
3. **Automatic Execution**:
   - 默认对所有发出的请求进行性能分析，无需额外开关。

## Non-Functional Requirements
- **Low Overhead**: 性能分析逻辑应极其轻量，不应显著增加请求的总耗时。
- **IntelliJ Consistency**: 界面风格需与 IDE 保持高度一致。

## Acceptance Criteria
- [ ] 请求完成后，用户可以在 "Profiling" 标签页中看到详细的耗时分解。
- [ ] 耗时数据准确反映了底层的网络行为。
- [ ] 即使在网络极快或极慢的情况下，UI 也能正常展示。

## Out of Scope
- JVM 内存占用分析。
- 多次请求之间的性能对比。
- 导出性能报告功能。
