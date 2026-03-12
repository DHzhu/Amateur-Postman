# OpenAPI to Collection Mapping Design

## 1. Overview

本文档定义了 OpenAPI 规范到 Amateur-Postman 内部 Collection 数据结构的映射逻辑。

## 2. Library Selection

**swagger-parser v2.1.32**

```kotlin
// build.gradle.kts
implementation("io.swagger.parser.v3:swagger-parser:2.1.32")
```

主要类：
- `OpenAPIV3Parser`: 直接解析 OpenAPI 3.0 文档
- `OpenAPIParser`: 通用解析器，支持自动版本检测
- `SwaggerParseResult`: 包含解析结果和验证信息
- `ParseOptions`: 解析选项（resolveFully, resolveCombinators 等）

## 3. Data Mapping

### 3.1 OpenAPI Document -> RequestCollection

| OpenAPI Field | Collection Field | Notes |
|--------------|------------------|-------|
| `info.title` | `name` | 集合名称 |
| `info.description` | `description` | 集合描述 |
| `info.version` | metadata | 可存储在扩展字段 |
| `servers` | `variables` | 提取 baseUrl 作为变量 |
| `components.securitySchemes` | `variables` | 提取 auth token 变量 |

### 3.2 OpenAPI Paths -> CollectionItem Structure

**分组策略**：按 `tags` 分组，每个 tag 对应一个 `Folder`

```
OpenAPI:
  paths:
    /users:
      get:
        tags: [User]
        summary: List users
      post:
        tags: [User]
        summary: Create user
    /orders:
      get:
        tags: [Order]
        summary: List orders

Collection:
  items:
    - Folder(name="User"):
        - Request(name="List users", method=GET, url=/users)
        - Request(name="Create user", method=POST, url=/users)
    - Folder(name="Order"):
        - Request(name="List orders", method=GET, url=/orders)
```

**无 tag 的处理**：放入名为 "Default" 的根级文件夹

### 3.3 OpenAPI Operation -> CollectionItem.Request

| OpenAPI Field | Request Field | Notes |
|--------------|---------------|-------|
| `summary` | `name` | 请求名称 |
| `description` | `description` | 请求描述 |
| `operationId` | metadata | 可用于 ID 生成 |
| HTTP method | `request.method` | HttpMethod enum |
| `servers[0].url + path` | `request.url` | 合并服务器地址与路径 |
| `parameters` | `request.headers`, `url` | 见 3.4 |
| `requestBody` | `request.body` | 见 3.5 |

### 3.4 OpenAPI Parameters -> Headers & URL

**Query Parameters**:
```kotlin
// OpenAPI: parameters with in: "query"
// Result: Append to URL as ?key=value&...
url = "$baseUrl$path?${queryParams.joinToString("&") { "${it.name}=${it.example ?: ""}" }}"
```

**Header Parameters**:
```kotlin
// OpenAPI: parameters with in: "header"
// Result: Add to headers map
headers[parameter.name] = parameter.example?.toString() ?: ""
```

**Path Parameters**:
```kotlin
// OpenAPI: parameters with in: "path"
// Result: Replace {param} in URL
url = url.replace("{${param.name}}", param.example?.toString() ?: "{${param.name}}")
```

### 3.5 OpenAPI RequestBody -> HttpBody

| Content-Type | BodyType | Handling |
|-------------|----------|----------|
| `application/json` | `JSON` | 提取 schema 生成示例 JSON |
| `application/xml` | `XML` | 提取 schema 生成示例 XML |
| `text/plain` | `TEXT` | 直接使用 |
| `multipart/form-data` | `MULTIPART` | 构建 MultipartPart 列表 |
| `application/x-www-form-urlencoded` | `FORM_URLENCODED` | 构建 form 数据 |

**Schema -> Example JSON Generation**:
```kotlin
fun generateExampleFromSchema(schema: Schema<*>): String {
    return when (schema) {
        is ObjectSchema -> {
            val properties = schema.properties.mapValues { (_, prop) ->
                generateExampleFromSchema(prop)
            }
            Gson().toJson(properties)
        }
        is StringSchema -> schema.example ?: "\"string\""
        is IntegerSchema -> schema.example ?: "0"
        is BooleanSchema -> schema.example ?: "false"
        // ... handle other types
    }
}
```

## 4. Implementation Architecture

```
OpenApiImporter
├── OpenApiParser (解析 OpenAPI 文档)
│   ├── parseFromUrl(url: String): OpenAPI
│   ├── parseFromFile(file: File): OpenAPI
│   └── parseFromContent(content: String): OpenAPI
│
├── OpenApiConverter (转换为 Collection)
│   ├── convert(openAPI: OpenAPI): RequestCollection
│   ├── convertPaths(paths: Paths): List<CollectionItem>
│   ├── convertOperation(path: String, operation: Operation): CollectionItem.Request
│   └── generateBodyExample(requestBody: RequestBody): HttpBody?
│
└── OpenApiSyncService (同步服务)
    ├── bindCollection(collectionId: String, openApiSource: String)
    ├── syncCollection(collectionId: String): SyncResult
    └── detectChanges(local: Collection, remote: OpenAPI): List<Change>
```

## 5. Edge Cases

### 5.1 多服务器配置
- 优先使用第一个 server
- 其余 servers 存储为变量供用户选择

### 5.2 $ref 引用
- 使用 `ParseOptions.setResolveFully(true)` 自动解析

### 5.3 安全配置
- 提取 `securitySchemes` 作为环境变量建议
- 支持 API Key, Bearer Token, Basic Auth

### 5.4 响应示例
- 可选：提取 `responses` 作为 Test Script 的断言模板

## 6. PSI Integration (Phase 4)

### 6.1 Target Annotations

```kotlin
val SPRING_ANNOTATIONS = listOf(
    "org.springframework.web.bind.annotation.RestController",
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping"
)

val JAXRS_ANNOTATIONS = listOf(
    "javax.ws.rs.Path",
    "javax.ws.rs.GET",
    "javax.ws.rs.POST",
    "javax.ws.rs.PUT",
    "javax.ws.rs.DELETE"
)
```

### 6.2 LineMarkerProvider Structure

```kotlin
class AmControllerLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>
    ) {
        // 1. 检查是否为方法声明
        if (element !is PsiMethod) return

        // 2. 检查是否有 Controller 相关注解
        val mappingAnnotation = findMappingAnnotation(element) ?: return

        // 3. 提取路径和方法
        val (method, path) = extractHttpInfo(mappingAnnotation)

        // 4. 创建 Gutter Icon
        result.add(
            NavigationGutterIconBuilder.create(Icons.API)
                .setTooltipText("Send request in Amateur-Postman")
                .setTarget { /* 打开/创建请求 */ }
                .createLineMarkerInfo(element)
        )
    }
}
```

### 6.3 plugin.xml Registration

```xml
<extensions defaultExtensionNs="com.intellij">
    <codeInsight.lineMarkerProvider
        language="JAVA"
        implementationClass="com.github.dhzhu.amateurpostman.codeinsight.AmControllerLineMarkerProvider"/>
</extensions>
```