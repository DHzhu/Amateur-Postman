# Amateur-Postman Development Guide

本文档包含 **Amateur-Postman** JetBrains 插件项目的开发指南、架构说明和代码规范。

---

## 📋 项目概述

**Amateur-Postman** 是一个类似 Postman 的 HTTP 客户端 JetBrains IDE 插件，提供完整的 HTTP 请求测试功能。

- **语言**: Kotlin
- **目标 JVM**: Java 21
- **目标 IDE**: IntelliJ IDEA 2025.1.1+
- **UI 框架**: Swing + IntelliJ Platform UI
- **网络库**: OkHttp 4.12.0
- **异步处理**: Kotlin Coroutines 1.9.0

---

## 🚀 构建和运行命令

### 核心命令

```bash
# 编译项目（快速检查编译错误）
./gradlew classes

# 完整构建（包括测试）
./gradlew build

# 运行测试套件
./gradlew test

# 构建插件（生成 ZIP 归档）
./gradlew buildPlugin

# 验证插件兼容性
./gradlew verifyPlugin

# 运行开发 IDE 实例（加载插件）
./gradlew runIde

# 清理构建产物
./gradlew clean
```

### 开发工作流

```bash
# 1. 修改代码
# 2. 检查编译
./gradlew classes

# 3. 运行测试
./gradlew test --tests "*CurlParserTest"

# 4. 运行 IDE 测试插件
./gradlew runIde

# 5. 构建分发版本
./gradlew buildPlugin
```

### 调试命令

```bash
# 运行特定测试类
./gradlew test --tests "com.github.dhzhu.amateurpostman.utils.CurlParserTest"

# 查看测试报告
open build/reports/tests/test/index.html

# 查看 HTML 测试覆盖率报告
./gradlew koverHtmlReport
open build/reports/kover/html/index.html

# 运行带 UI 测试的 IDE 实例
./gradlew runIdeForUiTests
```

---

## 🏗️ 核心架构

### 分层架构

```
┌─────────────────────────────────────────┐
│          UI Layer (Swing)               │
│  ┌──────────────┬──────────────────────┐│
│  │ ToolWindow   │  Panels (History,    ││
│  │ Factory      │  Request/Response)   ││
│  └──────────────┴──────────────────────┘│
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│        Service Layer                    │
│  ┌──────────────┬──────────────────────┐│
│  │ HTTP Request │  Request History     ││
│  │ Service      │  Service             ││
│  └──────────────┴──────────────────────┘│
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Data Models                     │
│  HttpRequest, HttpResponse, History     │
└─────────────────────────────────────────┘
```

### 核心类职责

#### **UI 层**

| 类 | 路径 | 职责 |
|---|---|---|
| `PostmanToolWindowFactory` | `toolWindow/PostmanToolWindowFactory.kt` | 实现 `ToolWindowFactory`，创建工具窗口实例 |
| `PostmanToolWindowPanel` | `ui/PostmanToolWindowPanel.kt` | 主 UI 面板，包含所有请求/响应界面组件 |
| `HistoryPanel` | `ui/HistoryPanel.kt` | 历史记录列表和管理界面 |

#### **服务层**

| 类 | 路径 | 职责 |
|---|---|---|
| `HttpRequestService` | `services/HttpRequestService.kt` | HTTP 请求服务接口 |
| `HttpRequestServiceImpl` | `services/HttpRequestServiceImpl.kt` | 使用 OkHttp 实现 HTTP 请求执行 |
| `RequestHistoryService` | `services/RequestHistoryService.kt` | 持久化和管理请求历史（最多 100 条） |

#### **数据模型**

| 类 | 路径 | 说明 |
|---|---|---|
| `HttpRequest` | `models/HttpModels.kt` | HTTP 请求数据类（URL、方法、headers、body） |
| `HttpResponse` | `models/HttpModels.kt` | HTTP 响应数据类（状态码、headers、body、耗时） |
| `HttpMethod` | `models/HttpModels.kt` | 枚举：GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS |
| `RequestHistoryEntry` | `models/RequestHistoryEntry.kt` | 历史记录条目（请求 + 响应 + 时间戳） |

#### **工具类**

| 类 | 路径 | 职责 |
|---|---|---|
| `CurlParser` | `utils/CurlParser.kt` | 解析 cURL 命令为 `HttpRequest` |
| `CurlExporter` | `utils/CurlExporter.kt` | 将 `HttpRequest` 导出为 cURL 命令 |
| `SyntaxHighlighter` | `utils/SyntaxHighlighter.kt` | JSON 语法高亮和格式化 |

### 插件配置

**plugin.xml** (`src/main/resources/META-INF/plugin.xml`)

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- 工具窗口 -->
    <toolWindow factoryClass="...PostmanToolWindowFactory"
               id="Amateur-Postman" anchor="right"/>

    <!-- 项目服务 -->
    <projectService serviceInterface="...HttpRequestService"
                   serviceImplementation="...HttpRequestServiceImpl"/>

    <projectService serviceImplementation="...RequestHistoryService"/>
</extensions>
```

---

## 🎨 代码风格指南

### Kotlin 代码规范

#### **命名约定**

```kotlin
// 类名：PascalCase
class HttpRequestService
class PostmanToolWindowPanel

// 函数名：camelCase
fun executeRequest()
fun formatJson()

// 常量：UPPER_SNAKE_CASE
const val MAX_HISTORY_SIZE = 100

// 变量：camelCase
val selectedMethod = HttpMethod.GET
private var currentRequestJob: Job? = null
```

#### **KDoc 注释要求**

**必须** 为所有公共 API 添加 KDoc 注释：

```kotlin
/**
 * Executes an HTTP request asynchronously using OkHttp.
 *
 * This method is suspendable and will run on the IO dispatcher.
 * It handles request cancellation gracefully and returns a detailed
 * response including status, headers, body, and timing information.
 *
 * @param request The HTTP request to execute
 * @return HttpResponse containing status, headers, body, and duration
 * @throws Exception if the request fails due to network or parsing errors
 */
suspend fun executeRequest(request: HttpRequest): HttpResponse
```

**简短注释格式**（适用于简单数据类）：

```kotlin
/** Represents an HTTP request to be executed */
data class HttpRequest(
    val url: String,
    val method: HttpMethod,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val contentType: String? = null
)
```

#### **数据类优先**

对于数据容器，使用 `data class`：

```kotlin
// ✅ 推荐
data class HttpResponse(
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, List<String>>,
    val body: String,
    val duration: Long,
    val isSuccessful: Boolean = statusCode in 200..299
)

// ❌ 避免（纯数据容器不需要普通类）
class HttpResponse(val statusCode: Int, ...)
```

---

### Swing/UI 线程规则

#### **核心规则：所有 UI 操作必须在 EDT（Event Dispatch Thread）中执行**

```kotlin
// ✅ 正确：使用 Swing Coroutine Scope
private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

private fun sendRequest() {
    scope.launch {
        // 自动在 EDT 中执行
        statusLabel.text = "Sending request..."
        sendButton.text = "Cancel"
    }
}

// ✅ 正确：显式切换到 EDT
private suspend fun updateUI(response: HttpResponse) = withContext(Dispatchers.Swing) {
    responseTextPane.text = response.body
    statusLabel.text = "Status: ${response.statusCode}"
}

// ❌ 错误：在 IO 线程更新 UI
private suspend fun badExample() = withContext(Dispatchers.IO) {
    statusLabel.text = "Error" // 会抛出异常
}
```

#### **网络请求必须在 IO 线程执行**

```kotlin
// ✅ 正确：网络请求在 IO 线程
override suspend fun executeRequest(request: HttpRequest): HttpResponse =
    withContext(Dispatchers.IO) {
        client.newCall(okHttpRequest).execute().use { response ->
            // 处理响应
            HttpResponse(...)
        }
    }
```

#### **UI 组件初始化**

```kotlin
// ✅ 推荐：lateinit + lazy 初始化
private lateinit var urlField: JBTextField
private lateinit var methodComboBox: ComboBox<HttpMethod>

fun createPanel(): JPanel {
    urlField = JBTextField()
    methodComboBox = ComboBox(HttpMethod.entries.toTypedArray())
    // ...
}

// ❌ 避免：在构造函数中初始化重型 UI 组件
class MyPanel {
    private val table = JBTable() // 可能导致内存泄漏
}
```

---

### 异步和取消支持

#### **所有长时间操作必须可取消**

```kotlin
// ✅ 正确：跟踪并支持取消
private var currentRequestJob: Job? = null
private var isRequestInProgress = false

private fun sendRequest() {
    if (isRequestInProgress) {
        currentRequestJob?.cancel() // 取消现有请求
        return
    }

    currentRequestJob = scope.launch {
        try {
            val response = httpService.executeRequest(request)
            displayResponse(response)
        } catch (e: CancellationException) {
            // 正常取消，不处理
        } finally {
            isRequestInProgress = false
        }
    }
}
```

#### **在挂起点检查取消**

```kotlin
override suspend fun executeRequest(request: HttpRequest): HttpResponse =
    withContext(Dispatchers.IO) {
        coroutineContext.ensureActive() // 检查取消

        val startTime = System.currentTimeMillis()
        val response = client.newCall(okHttpRequest).execute().use {
            coroutineContext.ensureActive() // 再次检查
            // ...
        }
    }
```

---

### 服务和资源管理

#### **服务实现必须实现 Disposable**

```kotlin
@Service(Service.Level.PROJECT)
class HttpRequestServiceImpl(private val project: Project)
    : HttpRequestService, Disposable {

    private val client: OkHttpClient by lazy { /* ... */ }

    override fun dispose() {
        // 释放资源
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        client.cache?.close()
    }
}
```

#### **服务获取方式**

```kotlin
// ✅ 推荐：使用扩展函数
private val httpService = project.service<HttpRequestService>()

// ❌ 避免：直接获取服务实例
private val httpService = project.getService(HttpRequestService::class.java)
```

---

### 错误处理

#### **网络请求错误处理**

```kotlin
// ✅ 推荐：返回错误响应而非抛出异常
override suspend fun executeRequest(request: HttpRequest): HttpResponse =
    withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(okHttpRequest).execute()
            HttpResponse(statusCode = response.code, ...)
        } catch (e: Exception) {
            // 返回错误响应而不是抛出异常
            HttpResponse(
                statusCode = 0,
                statusMessage = e.message ?: "Request failed",
                body = "Error: ${e.message}\n\n${e.stackTraceToString()}",
                isSuccessful = false
            )
        }
    }
```

#### **UI 错误提示**

```kotlin
// ✅ 使用 IntelliJ 的 Messages API
try {
    formatRequestBody()
} catch (e: Exception) {
    Messages.showWarningDialog(
        project,
        "Invalid JSON: ${e.message}",
        "Format Error"
    )
}
```

---

### 日志记录

```kotlin
// ✅ 使用 IntelliJ 的日志系统
import com.intellij.openapi.diagnostic.thisLogger

class HttpRequestServiceImpl {
    private val logger = thisLogger()

    override suspend fun executeRequest(...) {
        logger.info("Executing ${request.method} request to ${request.url}")
        // ...
        logger.info("Request completed in ${duration}ms with status ${response.code}")
    }
}
```

---

### 测试规范

#### **单元测试结构**

```kotlin
class CurlParserTest {

    @Test
    fun `parse simple GET curl command`() {
        // Given
        val curlCommand = "curl https://api.example.com/users"

        // When
        val request = CurlParser.parse(curlCommand)

        // Then
        assertEquals("https://api.example.com/users", request.url)
        assertEquals(HttpMethod.GET, request.method)
    }

    @Test
    fun `parse POST request with headers and body`() {
        // ...
    }
}
```

#### **测试文件命名**

```
src/test/kotlin/com/github/dhzhu/amateurpostman/
├── models/
│   └── HttpModelsTest.kt         # 测试 HttpModels.kt
└── utils/
    ├── CurlParserTest.kt          # 测试 CurlParser.kt
    ├── CurlExporterTest.kt        # 测试 CurlExporter.kt
    └── SyntaxHighlighterTest.kt   # 测试 SyntaxHighlighter.kt
```

---

## 📦 依赖管理

项目使用 **Gradle Version Catalog** 管理依赖：

```kotlin
// build.gradle.kts
dependencies {
    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
```

---

## 🌍 环境和变量系统

### 概述

Amateur-Postman 支持完整的环境和变量管理系统，允许您:
- 创建多个环境（如 Development、Staging、Production）
- 定义环境变量和全局变量
- 在请求中使用变量语法 `{{variableName}}`
- 使用内置函数生成动态值

### 变量语法

#### 基本变量
```
{{variableName}}
```

#### 内置函数
```
{{$timestamp}}              → 1704067200000
{{$timestamp:yyyy-MM-dd}}    → 2024-01-01
{{$uuid}}                   → 550e8400-e29b-41d4-a716-446655440000
{{$guid}}                   → 550e8400e29b41d4a716446655440000
{{$randomInt}}              → 42
{{$randomInt:1,100}}        → 73 (范围)
{{$randomInt:5}}            → 12345 (5位数字)
{{$randomString:10}}        → aB3xY9kL2m
```

### 变量优先级

1. 当前环境变量（最高优先级）
2. 全局变量
3. 缺失变量保持原样（带警告日志）

### 使用示例

**设置环境**:
```
创建环境 "Development":
  baseUrl = https://dev.api.com
  token = dev123

创建环境 "Production":
  baseUrl = https://api.com
  token = prod456

设置全局变量:
  timeout = 30000
```

**在请求中使用变量**:
```
URL: {{baseUrl}}/users
Headers:
  Authorization: Bearer {{token}}
  X-Timeout: {{timeout}}
```

**当选择 "Development" 环境时**:
```
实际请求:
  URL: https://dev.api.com/users
  Headers:
    Authorization: Bearer dev123
    X-Timeout: 30000
```

### 架构组件

#### 数据模型 (`models/EnvironmentModels.kt`)

```kotlin
// 变量
data class Variable(
    val key: String,
    val value: String,
    val description: String = "",
    val enabled: Boolean = true
)

// 环境
data class Environment(
    val id: String,
    val name: String,
    val variables: List<Variable> = emptyList(),
    val isGlobal: Boolean = false
)
```

#### 服务层 (`services/EnvironmentService.kt`)

```kotlin
// CRUD 操作
environmentService.createEnvironment(name)
environmentService.getEnvironments()
environmentService.updateEnvironment(environment)
environmentService.deleteEnvironment(id)
environmentService.renameEnvironment(id, newName)

// 当前环境管理
environmentService.setCurrentEnvironment(id)
environmentService.getCurrentEnvironment()

// 全局变量
environmentService.setGlobalVariable(variable)
environmentService.getGlobalVariables()

// 变量解析（合并全局+环境）
environmentService.getAllVariables()
```

#### 变量解析器 (`utils/VariableResolver.kt`)

```kotlin
// 自动替换请求中的变量
val processedRequest = VariableResolver.substitute(request, variables)

// 字符串级别的替换
val result = VariableResolver.substituteVariables(text, variables)

// 验证变量
val missing = VariableResolver.validateVariables(request, variables)
```

#### HTTP 服务集成 (`services/HttpRequestServiceImpl.kt`)

每个 HTTP 请求在发送前自动进行变量替换：

```kotlin
val environmentService = project.service<EnvironmentService>()
val variables = environmentService.getAllVariables()
val processedRequest = VariableResolver.substitute(request, variables)
// 使用 processedRequest 发送实际请求
```

### UI 组件 (`ui/EnvironmentPanel.kt`)

**功能**:
- 环境选择下拉框
- 变量表格（Key-Value-Description-Enabled 列）
- 添加/删除变量按钮
- 管理环境对话框（创建/重命名/删除）
- 全局变量选项卡

**特性**:
- 实时编辑变量
- 案例不敏感的变量名
- 支持禁用变量（不参与替换）
- 自动持久化到 `amateur-postman-env.xml`

### 最佳实践

1. **使用有意义的变量名**
   ```
   ✅ baseUrl, apiToken, userId
   ❌ var1, temp, x
   ```

2. **为敏感数据使用环境变量**
   ```
   不要在代码中硬编码 API 密钥
   使用环境变量：{{apiKey}}
   ```

3. **利用内置函数生成动态值**
   ```
   每次请求生成唯一 ID：{{$uuid}}
   时间戳：{{$timestamp:yyyy-MM-dd HH:mm:ss}}
   ```

4. **组织变量层级**
   ```
   全局变量：通用配置（timeout, retryCount）
   环境变量：环境特定（baseUrl, apiKey）
   ```

---

## 🔧 开发最佳实践

### 1. 修改代码后立即检查编译

```bash
# Hook 自动执行，也可手动检查
./gradlew classes
```

### 2. 编写测试覆盖核心逻辑

```bash
# 运行特定测试
./gradlew test --tests "*CurlParserTest"
```

### 3. 使用 IntelliJ 的内置组件

```kotlin
// ✅ 推荐：使用 IntelliJ UI 组件
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable

// ❌ 避免：直接使用 Swing 组件
import javax.swing.JTable
import javax.swing.JTextField
```

### 4. 遵循插件资源管理

```kotlin
// 所有 Disposable 组件必须注册清理
class MyPanel : Disposable {
    override fun dispose() {
        scope.cancel() // 取消所有协程
    }
}
```

### 5. 键盘快捷键

插件已注册以下快捷键：
- `Ctrl+Enter` - 发送请求
- `Ctrl+L` - 清空响应
- `Escape` - 取消请求

---

## 📁 项目结构

```
Amateur-Postman/
├── build.gradle.kts                    # 构建配置
├── gradle/
│   └── libs.versions.toml             # 依赖版本目录
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/github/dhzhu/amateurpostman/
│   │   │       ├── models/            # 数据模型
│   │   │       ├── services/          # 服务层
│   │   │       ├── ui/                # UI 组件
│   │   │       ├── toolWindow/        # 工具窗口
│   │   │       └── utils/             # 工具类
│   │   └── resources/
│   │       └── META-INF/
│   │           └── plugin.xml         # 插件配置
│   └── test/
│       └── kotlin/                    # 测试代码
├── CLAUDE.md                           # 本文档
└── .claudecode.json                    # Claude Code Hook 配置
```

---

## 🔍 故障排查

### 编译错误

```bash
# 清理并重新构建
./gradlew clean build

# 检查 Kotlin 版本
./gradlew kotlinVersion
```

### 测试失败

```bash
# 查看详细测试输出
./gradlew test --info

# 查看测试报告
open build/reports/tests/test/index.html
```

### IDE 无法加载插件

```bash
# 验证插件兼容性
./gradlew verifyPlugin

# 检查 plugin.xml 配置
cat src/main/resources/META-INF/plugin.xml
```

---

## 📚 参考资源

- [IntelliJ Platform SDK DevGuide](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-overview.html)
- [Swing Threading Rules](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/)
