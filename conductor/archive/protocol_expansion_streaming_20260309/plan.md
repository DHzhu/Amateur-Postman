# Implementation Plan: Protocol Expansion (WebSocket & gRPC Streaming)

## Risk Assessment
- **UI 渲染压力**: 大量流式数据可能导致界面卡顿，需确保消息列表使用了虚拟滚动或批量渲染。
- **连接管理**: WebSocket 和 gRPC 流的长连接生命周期管理，需确保在关闭 Tab 或 IDE 时资源被正确释放。
- **gRPC 动态 Stub**: 流式调用在 `DynamicMessage` 转换上可能比 Unary 复杂，需要验证现有的 Protobuf 解析器是否支持。

## Development Phases

### Phase 1: WebSocket 基础实现
1. [x] 扩展 `HttpModels.kt` 增加 WebSocket 类型。
2. [x] 在 `HttpRequestService` 中增加 `connectWebSocket` 方法，返回 `WebSocket` 实例及其状态监听。
3. [x] 创建 `WebSocketPanel` UI，包含 URL 输入、Connect 按钮、消息输入框及历史列表。
4. [x] 验证 ws/wss 连接。

### Phase 2: gRPC Streaming 支持
1. [x] 修改 `GrpcEditorPanel`，增加流式状态指示器（Stream Status）。
2. [x] 扩展 `HttpRequestService` 以支持 gRPC 的流式调用监听。
3. [x] 实现 `ServerStreaming` 处理：循环接收 `StreamObserver` 的消息。
4. [x] 实现 `ClientStreaming` & `BiDiStreaming` 处理：允许用户多次点击 "Send" 发送消息。

### Phase 3: 优化与集成
1. [x] 为 WebSocket/Streaming 消息接入 `HighPerfResponseViewer` 提高渲染性能。
2. [x] 确保连接在 Tab 关闭或插件卸载时安全释放（`Disposable` 模式）。
3. [x] 单元测试与集成测试（使用 `MockWebServer` 模拟 WebSocket 服务器）。

## Definition of Done (DoD)
- [ ] 成功连接到测试 WebSocket Server 并发送/接收 100+ 条消息无卡顿。
- [ ] 完成 gRPC 4 种模式的调用测试。
- [ ] 所有新测试通过且覆盖率为 80%+。
- [ ] UI 符合 IntelliJ 原生风格。
