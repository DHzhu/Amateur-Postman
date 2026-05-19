# Track Spec: Protocol Expansion (WebSocket & gRPC Streaming)

## Overview
在 Phase 7 完成 gRPC Unary 支持的基础上，进一步扩展 Amateur-Postman 对长连接协议的支持，包括 WebSocket 协议和 gRPC 的流式调用（Streaming RPCs）。

## Success Criteria
1. **WebSocket 支持**:
    - 用户可以输入 WebSocket URL (ws/wss) 并建立连接。
    - 支持发送文本（Text）和二进制（Binary）消息。
    - 提供消息历史记录视图，支持消息内容格式化（JSON）。
    - 支持手动关闭连接。
2. **gRPC Streaming 支持**:
    - 支持 **Server-side Streaming**: 客户端发送一个请求，接收多个响应。
    - 支持 **Client-side Streaming**: 客户端发送多个请求，接收一个响应。
    - 支持 **Bidirectional Streaming**: 双方可自由发送/接收多条消息。
    - UI 适配：在 gRPC 编辑器中显示流式状态（正在流式传输、已结束）。
3. **稳定性与性能**:
    - 消息监听不阻塞 UI 线程。
    - 能够正确处理大量流式数据的展示（复用 HighPerfResponseViewer）。

## Technical Details
- **WebSocket**: 使用 OkHttp 的 `WebSocket` 接口实现。
- **gRPC Streaming**: 使用 `io.grpc.stub.StreamObserver` 处理异步流式调用。
- **UI**: 
    - WebSocket 专用面板或复用现有 RequestPanel 的 Body 部分发送消息。
    - 消息历史列表（类似聊天窗口或表格）。

## Scope & Constraints
- 初始版本不包含 WebSocket 的脚本断言支持（仅限手动调试）。
- gRPC Streaming 依赖现有的 Proto 动态解析逻辑。
