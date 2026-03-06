# Specification: Protocol Expansion (gRPC) Support

## 背景 (Context)

Amateur-Postman 目前仅支持标准 HTTP/REST 协议。随着现代微服务架构的普及，gRPC 已成为服务间通信的主流协议。为了保持插件的竞争力并扩展其应用场景，需要引入对 gRPC 协议的原生支持。

## 目标 (Goals)

1. **基础设施集成**: 在 Gradle 中引入 gRPC 与 Protobuf 相关的依赖及代码生成插件。
2. **Proto 解析与加载**: 支持从本地 `.proto` 文件或目录加载服务定义，并自动生成方法列表。
3. **Unary Call 实现**: 实现最基础的一元 RPC 调用（请求-响应模式）。
4. **Metadata & 变量集成**: 支持在 gRPC 请求中注入环境变量和 Metadata（Headers）。
5. **UI 适配**: 在请求编辑器中提供 gRPC 专用的交互界面（Service/Method 下拉选框及 JSON Body 编辑器）。

## 关键技术点 (Technical Details)

- **核心库**: 采用 `grpc-netty-shaded` 以避免与 IntelliJ 平台或其他插件的 Netty 版本冲突。
- **Proto 处理（已锁定方案：动态解析）**:
  - 采用 `com.google.protobuf:protobuf-java` 的 `DescriptorProtos` 进行运行时动态解析 `.proto` 文件。
  - 通过 `protoc` 命令行工具生成 `FileDescriptorSet`，插件端加载二进制描述符来提取 Service/Method 定义。
  - 不采用预编译代码生成（`protoc-gen-grpc-kotlin`），以避免对用户环境的编译工具链依赖。
  - 需处理 `.proto` 的 `import` 路径解析、嵌套 Message 展开以及 Well-Known Types（如 `google.protobuf.Timestamp`、`Struct` 等）的映射。
- **序列化**: 维持与现有脚本系统的兼容性，请求/响应 Body 统一采用 JSON 格式进行展示与编辑，内部通过 `com.google.protobuf:protobuf-java-util` 的 `JsonFormat` 进行 JSON ↔ Protobuf 双向转换。
- **并发模型**: 利用 Kotlin Coroutines 异步执行 gRPC 调用，确保 UI 流畅。

## 风险评估 (Risk Assessment)

- **版本冲突**: IntelliJ 平台自带多版本的库，gRPC 及其依赖（如 Guava, Netty）极易引发冲突。
- **动态解析复杂性**: 动态加载 `.proto` 文件比预编译更具挑战，需要处理复杂的依赖路径、Import 关系和 Well-Known Types 映射。此为本轨道最高技术风险，时间估算需留有余量。
- **流式处理 (Streaming)**: 服务端流、客户端流及双向流的 UI 交互与生命周期管理较为复杂，初期仅实现 Unary Call。

## 范围排除 (Out of Scope)

- **WebSocket 支持**: Phase 7 路线图标题为 "gRPC & WebSocket"，但 WebSocket 将拆分为独立轨道 (`websocket_support`) 在本轨道完成后启动，不在本次迭代范围内。
- **gRPC 流式调用**: Server Streaming / Client Streaming / Bidirectional Streaming 均推迟至后续 gRPC 增强轨道。
