# Tech Stack - Amateur-Postman

## Core Technologies
- **Language**: Kotlin (JVM 21)
- **Framework**: IntelliJ Platform SDK (Plugin Development)
- **Build Tool**: Gradle (Kotlin DSL) with IntelliJ Platform Gradle Plugin (v2.x)

## Libraries
- **HTTP Client**: [OkHttp 4.12.0](https://square.github.io/okhttp/) - 用于执行 HTTP 请求。
- **Concurrency**: [Kotlin Coroutines 1.9.0](https://github.com/Kotlin/kotlinx.coroutines) - 用于异步处理请求，避免阻塞 UI 线程。
- **JSON Parsing**: [Gson 2.11.0](https://github.com/google/gson) - 用于解析请求/响应体以及持久化配置。
- **UI Architecture**: Swing (IntelliJ Platform 原生)

## Quality & Testing
- **Test Framework**: JUnit 5, IntelliJ Test Framework
- **Mocking**: MockWebServer (OkHttp) - 用于模拟服务器响应。
- **Static Analysis**: Qodana, Checkstyle (via build plugins)
- **Coverage**: Kover
