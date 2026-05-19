# 技术栈 - Amateur-Postman

## 核心技术
- **语言**: Kotlin (JVM 21)
- **框架**: IntelliJ 平台 SDK（插件开发）
- **构建工具**: Gradle (Kotlin DSL) + IntelliJ 平台 Gradle 插件 (v2.x)

## 依赖库
- **gRPC 客户端**: [grpc-java 1.65.1](https://github.com/grpc/grpc-java) - 动态 Unary gRPC 调用（`grpc-netty-shaded`、`grpc-stub`、`grpc-protobuf`、`grpc-inprocess`）。
- **Protobuf**: [protobuf-java 4.27.2](https://github.com/protocolbuffers/protobuf) - 动态解析 `.proto` 文件并进行 JSON ↔ DynamicMessage 转换（`protobuf-java-util`）。
- **HTTP 客户端**: [OkHttp 4.12.0](https://square.github.io/okhttp/) - 用于执行 HTTP 请求。
- **并发**: [Kotlin Coroutines 1.9.0](https://github.com/Kotlin/kotlinx.coroutines) - 用于异步处理请求，避免阻塞 UI 线程。
- **JSON 解析**: [Jackson 2.18.2](https://github.com/FasterXML/jackson) - 用于解析请求/响应体以及持久化配置（`jackson-databind` + `jackson-module-kotlin`），由 `JsonService` 单例管理。
- **脚本引擎**: [GraalVM JS 24.1.2](https://github.com/oracle/graaljs) - 用于在 JDK 21+ 环境下执行前置脚本和测试脚本（替代已废弃的 Nashorn）。
- **UI 架构**: Swing（IntelliJ 平台原生）

## 质量与测试
- **测试框架**: JUnit 5、IntelliJ 测试框架
- **模拟服务**: MockWebServer (OkHttp)、gRPC InProcessServer - 分别用于模拟 HTTP 服务器和内嵌 gRPC 服务器。
- **静态分析**: Qodana、Checkstyle（通过构建插件）
- **覆盖率**: Kover

## 构建质量门禁（提交前必须通过）

| 检查项 | 命令 | 通过标准 |
|--------|------|----------|
| 编译零警告 | `./gradlew clean compileKotlin compileTestKotlin --no-build-cache` | 输出中无 `^w:` 行 |
| 全量测试 | `./gradlew test` | 构建成功，0 失败 |
| 插件验证器 | `./gradlew verifyPlugin` | 结果为兼容，无内部 API 警告 |
| 插件打包 | `./gradlew buildPlugin` | 生成 zip 文件 |

**注意**: 编译缓存会静默吞掉警告，检查警告时**必须**加 `--no-build-cache`。
