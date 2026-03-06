# Implementation Plan: Protocol Expansion (gRPC) Support

## 执行策略（风险优先）

- 优先处理 **库依赖冲突 (IntelliJ Platform vs gRPC/Netty)**。
- 其次验证 **Proto 动态解析**，这是决定用户体验的关键点，也是最高风险项。
- 采用 **Unary Call First** 策略，暂时不涉及流式处理。

## 阶段 0: 基础设施与依赖验证 ✅ DONE

- [x] **步骤 0.1**: 更新 `gradle/libs.versions.toml`，新增以下锁定版本：
  - `grpc = "1.65.1"` (`io.grpc:grpc-netty-shaded`, `io.grpc:grpc-protobuf`, `io.grpc:grpc-stub`)
  - `protobuf = "4.27.2"` (`com.google.protobuf:protobuf-java`, `com.google.protobuf:protobuf-java-util`)
  - `grpc-testing = "1.65.1"` (`io.grpc:grpc-testing`), `grpc-inprocess = "1.65.1"`
- [x] **步骤 0.2**: 修改 `build.gradle.kts` 引入依赖，运行 `./gradlew classes` — 编译通过，无冲突。
- [x] **步骤 0.3**: 建立 `GrpcInfrastructureTest` — ManagedChannel 和 InProcessServer 均验证通过。
- [ ] **步骤 0.4**: 运行 `./gradlew verifyPlugin`（推迟到阶段 4 统一验证）

**完成标准（DoD）**

- [x] Gradle 编译通过，无冲突报告。
- [x] `GrpcInfrastructureTest` 成功实例化 `Channel` 并执行 `shutdownNow()`。
- [ ] Plugin Verifier 无新增 ERROR 级别报告。

## 阶段 1: Proto 动态解析实现 ✅ DONE

- [x] **步骤 1.1**: 实现 `ProtoParser` 服务，支持加载指定路径的 `.proto` 文件。
  - 调用 `protoc --descriptor_set_out` 生成 `FileDescriptorSet` 二进制文件。
  - 回退方案：`parseFromDescriptorSet()` 支持加载预生成的描述符文件；无 protoc 时提供友好错误信息。
- [x] **步骤 1.2**: 实现 `FileDescriptorSet` 解析器，从二进制描述符中提取 Service 和 Method 列表。
  - 处理 `import` 依赖链（递归解析 imported proto 定义）。
  - 支持 Well-Known Types（`google.protobuf.Timestamp`, `Struct`, `StringValue` 等）的自动映射。
- [x] **步骤 1.3**: 使用 `com.google.protobuf:protobuf-java-util` 的 `JsonFormat` 实现 JSON ↔ Protobuf `DynamicMessage` 双向转换。
- [x] **步骤 1.4**: 编写覆盖不同复杂度的测试 Proto 文件（greeter/nested/well_known/multi_service）。

**完成标准（DoD）**

- [x] 单元测试覆盖 4 种复杂度等级：8/8 通过。
- [x] 给定 Proto 文件，能正确列出所有 gRPC 服务及方法签名。
- [x] JSON ↔ DynamicMessage 转换测试通过（含 Well-Known Types）。

## 阶段 2: 核心一元调用服务实现 (Unary Call) ✅ DONE

- [x] **步骤 2.1**: 实现 `GrpcRequestService`，基于 `DynamicMessage` + `ProtoUtils.marshaller` 构造动态 Unary 调用，Channel 缓存复用。
- [x] **步骤 2.2**: 集成 `Metadata` 处理逻辑，支持添加自定义头信息（`MetadataUtils.newAttachHeadersInterceptor`）。
- [x] **步骤 2.3**: 错误处理机制：`StatusRuntimeException` 捕获，映射状态码为用户可读消息（`GrpcCallResult.Failure.userMessage`）。
- [x] **步骤 2.4**: 集成测试 `GrpcRequestServiceTest`，使用 `InProcessServer` + 手动 `ServerServiceDefinition` 端到端验证。

**完成标准（DoD）**

- [x] 集成测试：通过 `InProcessServer` 完成完整 Unary RPC 请求-响应周期：5/5 通过。
- [x] 响应支持 JSON 展示。
- [x] gRPC 状态码错误能映射为用户可读的错误消息。

## 阶段 3: UI 适配与变量集成 ✅ DONE

- [x] **步骤 3.1**: 在 `PostmanToolWindowFactory` 中增加顶层协议 Tab（HTTP / gRPC），非侵入式集成，原有 HTTP 功能零改动。
- [x] **步骤 3.2**: `GrpcEditorPanel` 中开发 `ProtoFileChooserPanel`：使用 IntelliJ `FileChooser.chooseFile()` 选择 `.proto` 文件，支持记忆路径。
- [x] **步骤 3.3**: `GrpcEditorPanel` 中开发 `GrpcServiceMethodPanel`：Service/Method 联动下拉（只显示 Unary 方法），选择 Proto 文件后自动刷新列表。
- [x] **步骤 3.4**: `GrpcEditorPanel` 中开发 `GrpcBodyEditorPanel`：JSON Body 编辑区，根据选定 Method 的 Input Message 提供空模板（Generate Template 按钮）。
- [x] **步骤 3.5**: 响应区展示 gRPC 响应（JSON 格式，自动美化），新增 gRPC Status 与 Trailing Metadata 显示区域。
- [x] **步骤 3.6**: 在 `GrpcEditorPanel.sendGrpcRequest()` 中调用 `EnvironmentService.getAllVariables()` + `VariableResolver` 解析 Metadata 和 Body 中的环境变量。

**完成标准（DoD）**

- [x] 用户可以通过 UI 选择 Proto 文件并触发 Unary gRPC 请求。
- [x] Service/Method 下拉联动刷新功能正常。
- [x] 环境变量在 gRPC 调用中生效。

## 阶段 4: 总体验收与文档（2h - 3h）

- [ ] **步骤 4.1**: 运行全量测试 `./gradlew test`（含原有 HTTP 测试回归）。
- [ ] **步骤 4.2**: 运行 `./gradlew verifyPlugin` 最终确认兼容性。
- [ ] **步骤 4.3**: 更新 `README.md`（路线图、特性列表）。
- [ ] **步骤 4.4**: 更新 `conductor/tech-stack.md`（新增 gRPC/Protobuf 依赖描述）。
- [ ] **步骤 4.5**: 归档轨道。

**回归命令**

```bash
./gradlew test                          # 全量回归
./gradlew test --tests "*Grpc*"         # gRPC 专项测试
./gradlew test --tests "*Proto*"        # Proto 解析测试
./gradlew verifyPlugin                  # Plugin 兼容性验证
```

**完成标准（DoD）**

- [ ] 所有测试全绿（含原有 HTTP 测试无回归）。
- [ ] Plugin Verifier 无 ERROR。
- [ ] 核心功能演示通过。
- [ ] 文档已更新。
