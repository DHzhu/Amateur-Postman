# Tech Stack - Amateur-Postman

## Core Technologies
- **Language**: Kotlin (JVM 21)
- **Framework**: IntelliJ Platform SDK (Plugin Development)
- **Build Tool**: Gradle (Kotlin DSL) with IntelliJ Platform Gradle Plugin (v2.x)

## Libraries
- **gRPC Client**: [grpc-java 1.65.1](https://github.com/grpc/grpc-java) - Dynamic Unary gRPC calls (`grpc-netty-shaded`, `grpc-stub`, `grpc-protobuf`, `grpc-inprocess`).
- **Protobuf**: [protobuf-java 4.27.2](https://github.com/protocolbuffers/protobuf) - Dynamic `.proto` parsing and JSON ↔ DynamicMessage conversion (`protobuf-java-util`).
- **HTTP Client**: [OkHttp 4.12.0](https://square.github.io/okhttp/) - HTTP request execution.
- **Concurrency**: [Kotlin Coroutines 1.9.0](https://github.com/Kotlin/kotlinx.coroutines) - Async request processing without blocking the UI thread.
- **JSON Parsing**: [Jackson 2.18.2](https://github.com/FasterXML/jackson) - Request/response body parsing and configuration persistence (`jackson-databind` + `jackson-module-kotlin`), managed by `JsonService` singleton.
- **Scripting Engine**: [GraalVM JS 24.1.2](https://github.com/oracle/graaljs) - Pre-request and test script execution on JDK 21+ (replaces deprecated Nashorn).
- **UI Architecture**: Swing (IntelliJ Platform native)

## Quality & Testing
- **Test Framework**: JUnit 5, IntelliJ Test Framework
- **Mocking**: MockWebServer (OkHttp), gRPC InProcessServer - for simulating HTTP and embedded gRPC servers respectively.
- **Static Analysis**: Qodana, Checkstyle (via build plugins)
- **Coverage**: Kover

## Build Quality Gates (Must Pass Before Commit)

| Check | Command | Pass Criteria |
|-------|---------|---------------|
| Zero compiler warnings | `./gradlew clean compileKotlin compileTestKotlin --no-build-cache` | No `^w:` lines in output |
| Full test suite | `./gradlew test` | BUILD SUCCESSFUL, 0 failures |
| Plugin Verifier | `./gradlew verifyPlugin` | Result: Compatible, no Internal API warnings |
| Plugin packaging | `./gradlew buildPlugin` | Zip file generated |

**Note**: Build cache silently swallows warnings. Always use `--no-build-cache` when checking for warnings.
