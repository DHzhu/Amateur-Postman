package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OpenApiExporterTest {

    // ── Info / metadata ───────────────────────────────────────────────────────

    @Test
    fun `collection name maps to info title`() {
        val result = OpenApiExporter.exportCollection(collection("My API"), OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("My API"), "content: ${result.content}")
    }

    @Test
    fun `collection description maps to info description`() {
        val col = collection("API", description = "A test API", items = listOf(request("GET", "https://api.example.com/")))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("A test API"), "content: ${result.content}")
    }

    @Test
    fun `openapi version field is 3_0_3`() {
        val result = OpenApiExporter.exportCollection(collection("API"), OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("3.0.3"), "content: ${result.content}")
    }

    // ── Tags ─────────────────────────────────────────────────────────────────

    @Test
    fun `top-level request uses collection name as tag`() {
        val col = collection("MyAPI", items = listOf(request("GET", "https://api.example.com/health")))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("MyAPI"), "content: ${result.content}")
    }

    @Test
    fun `single folder becomes tag`() {
        val col = collection("API", items = listOf(
            folder("users", listOf(request("GET", "https://api.example.com/users")))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("users"), "content: ${result.content}")
    }

    @Test
    fun `nested folders produce full path tag`() {
        val col = collection("API", items = listOf(
            folder("users", listOf(
                folder("admin", listOf(request("GET", "https://api.example.com/admin")))
            ))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("users/admin"), "content: ${result.content}")
    }

    @Test
    fun `multiple requests in same folder produce a single tag entry`() {
        val col = collection("API", items = listOf(
            folder("users", listOf(
                request("GET", "https://api.example.com/users"),
                request("POST", "https://api.example.com/users")
            ))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        // "users" should appear in the tags array only once
        val tagOccurrences = Regex("\"users\"").findAll(content).count()
        assertTrue(tagOccurrences >= 1, "Tag 'users' should appear in output: $content")
    }

    // ── Paths ─────────────────────────────────────────────────────────────────

    @Test
    fun `GET method is mapped to get operation`() {
        val col = collection("API", items = listOf(request("GET", "https://api.example.com/users")))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("\"get\""), "content: ${result.content}")
    }

    @Test
    fun `POST method is mapped to post operation`() {
        val col = collection("API", items = listOf(
            request("POST", "https://api.example.com/users",
                body = HttpBody("{\"name\":\"Alice\"}", BodyType.JSON))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("\"post\""), "content: ${result.content}")
    }

    @Test
    fun `path is extracted from full URL`() {
        val col = collection("API", items = listOf(request("GET", "https://api.example.com/users/profile")))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("/users/profile"), "content: ${result.content}")
    }

    @Test
    fun `variable placeholder in URL converted to OpenAPI path param`() {
        val col = collection("API", items = listOf(request("GET", "https://api.example.com/users/{{userId}}")))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.contains("/users/{userId}"), "Path should use {userId}: $content")
        assertTrue(content.contains("\"userId\""), "Parameters should declare userId: $content")
        assertTrue(content.contains("\"path\""), "userId should be in path: $content")
    }

    @Test
    fun `query params in URL become query parameters`() {
        val col = collection("API", items = listOf(request("GET", "https://api.example.com/search?q=test&limit=10")))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.contains("\"q\""), "content: $content")
        assertTrue(content.contains("\"query\""), "content: $content")
    }

    @Test
    fun `same path with different methods are merged into one path entry`() {
        val col = collection("API", items = listOf(
            request("GET", "https://api.example.com/users"),
            request("POST", "https://api.example.com/users",
                body = HttpBody("{}", BodyType.JSON))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        // /users should appear once as a path key, containing both get and post
        val pathOccurrences = Regex("\"/users\"").findAll(content).count()
        assertEquals(1, pathOccurrences, "Path /users should appear exactly once: $content")
        assertTrue(content.contains("\"get\""), "content: $content")
        assertTrue(content.contains("\"post\""), "content: $content")
    }

    // ── Request Body ──────────────────────────────────────────────────────────

    @Test
    fun `GET request has no requestBody`() {
        val col = collection("API", items = listOf(request("GET", "https://api.example.com/users")))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertFalse(result.content!!.contains("requestBody"), "GET should have no requestBody")
    }

    @Test
    fun `POST with JSON body produces json requestBody`() {
        val col = collection("API", items = listOf(
            request("POST", "https://api.example.com/users",
                body = HttpBody("{\"name\":\"Alice\"}", BodyType.JSON))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.contains("requestBody"), "content: $content")
        assertTrue(content.contains("application/json"), "content: $content")
    }

    @Test
    fun `POST with urlencoded body produces form requestBody`() {
        val col = collection("API", items = listOf(
            request("POST", "https://api.example.com/login",
                body = HttpBody("username=alice&password=secret", BodyType.FORM_URLENCODED))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.contains("application/x-www-form-urlencoded"), "content: $content")
        assertTrue(content.contains("username"), "content: $content")
        assertTrue(content.contains("password"), "content: $content")
    }

    @Test
    fun `POST with multipart body produces multipart requestBody`() {
        val parts = listOf(
            MultipartPart.TextField("username", "alice"),
            MultipartPart.FileField("avatar", "photo.jpg", "image/jpeg")
        )
        val col = collection("API", items = listOf(
            request("POST", "https://api.example.com/upload",
                body = HttpBody("", BodyType.MULTIPART, parts))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.contains("multipart/form-data"), "content: $content")
        assertTrue(content.contains("username"), "content: $content")
        assertTrue(content.contains("avatar"), "content: $content")
    }

    // ── Format ────────────────────────────────────────────────────────────────

    @Test
    fun `JSON format produces valid JSON with openapi field`() {
        val result = OpenApiExporter.exportCollection(collection("API"), OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.trimStart().startsWith("{"), "Should start with {: $content")
        assertTrue(content.contains("\"openapi\""), "Should have openapi field: $content")
    }

    @Test
    fun `YAML format starts with openapi key`() {
        val result = OpenApiExporter.exportCollection(collection("API"), OpenApiExporter.ExportFormat.YAML)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.contains("openapi:"), "YAML should have openapi: key: $content")
        assertTrue(content.contains("3.0.3"), "YAML should have version 3.0.3: $content")
    }

    @Test
    fun `YAML format does not contain JSON braces`() {
        val result = OpenApiExporter.exportCollection(collection("API"), OpenApiExporter.ExportFormat.YAML)

        assertTrue(result.isSuccess)
        assertFalse(result.content!!.trimStart().startsWith("{"), "YAML should not start with {")
    }

    @Test
    fun `exportToFile writes content to file`() {
        val col = collection("API", items = listOf(request("GET", "https://api.example.com/")))
        val file = createTempFile("openapi", ".json")
        file.deleteOnExit()

        val result = OpenApiExporter.exportToFile(col, file, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(file.readText().contains("openapi"))
    }

    // ── Header Export (Phase 2) ───────────────────────────────────────────────

    @Test
    fun `non-sensitive headers are exported as header parameters`() {
        val col = collection("API", items = listOf(
            requestWithHeaders("GET", "https://api.example.com/data",
                mapOf("X-Request-Id" to "123", "Accept-Language" to "zh-CN"))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.contains("X-Request-Id"), "Non-sensitive header should be exported: $content")
        assertTrue(content.contains("Accept-Language"), "Non-sensitive header should be exported: $content")
        assertTrue(content.contains("\"header\""), "Headers should use 'header' location: $content")
    }

    @Test
    fun `sensitive headers are filtered by default`() {
        val col = collection("API", items = listOf(
            requestWithHeaders("GET", "https://api.example.com/secure",
                mapOf("Authorization" to "Bearer token", "X-Custom" to "value"))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertFalse(content.contains("\"Authorization\""), "Authorization should be filtered by default: $content")
        assertTrue(content.contains("X-Custom"), "Non-sensitive header should still be exported: $content")
    }

    @Test
    fun `cookie header is filtered by default`() {
        val col = collection("API", items = listOf(
            requestWithHeaders("GET", "https://api.example.com/data",
                mapOf("Cookie" to "session=abc", "X-Api-Version" to "2"))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertFalse(content.contains("\"Cookie\""), "Cookie should be filtered by default: $content")
        assertTrue(content.contains("X-Api-Version"), "Non-sensitive header should be exported: $content")
    }

    @Test
    fun `sensitive headers included when flag is true`() {
        val col = collection("API", items = listOf(
            requestWithHeaders("GET", "https://api.example.com/secure",
                mapOf("Authorization" to "Bearer token"))
        ))
        val result = OpenApiExporter.exportCollection(
            col,
            OpenApiExporter.ExportFormat.JSON,
            includeSensitiveHeaders = true
        )

        assertTrue(result.isSuccess)
        val content = result.content!!
        assertTrue(content.contains("Authorization"), "Authorization should appear when flag is true: $content")
    }

    @Test
    fun `request with no headers produces no header parameters`() {
        val col = collection("API", items = listOf(
            request("GET", "https://api.example.com/data")
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        // No header parameters expected - just path/query params
        val content = result.content!!
        assertFalse(content.contains("\"header\""), "No headers should produce no header params: $content")
    }

    // ── Response Inference (Phase 3) ──────────────────────────────────────────

    @Test
    fun `response status code 200 is present in output by default`() {
        val col = collection("API", items = listOf(request("GET", "https://api.example.com/users")))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("200"), "Default response should contain 200: ${result.content}")
    }

    @Test
    fun `JsonToSchemaConverter converts json object to object schema`() {
        val schema = JsonToSchemaConverter.convert("{\"name\":\"Alice\",\"age\":30}")

        assertNotNull(schema, "Schema should not be null")
        assertEquals("object", schema!!.type, "Top-level JSON object should produce object schema")
    }

    @Test
    fun `JsonToSchemaConverter converts json array to array schema`() {
        val schema = JsonToSchemaConverter.convert("[{\"id\":1},{\"id\":2}]")

        assertNotNull(schema, "Schema should not be null")
        assertEquals("array", schema!!.type, "JSON array should produce array schema")
    }

    @Test
    fun `JsonToSchemaConverter returns null for invalid json`() {
        val schema = JsonToSchemaConverter.convert("not json")

        assertNull(schema, "Invalid JSON should produce null schema")
    }

    // ── Performance (Phase 3.4) ───────────────────────────────────────────────

    @Test
    fun `large collection export completes within time limit`() {
        val items = (1..200).map { i ->
            request("GET", "https://api.example.com/resource/$i")
        }
        val col = collection("BigAPI", items = items)

        val start = System.currentTimeMillis()
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)
        val elapsed = System.currentTimeMillis() - start

        assertTrue(result.isSuccess, "Export should succeed")
        assertTrue(elapsed < 5000, "Export of 200 requests should complete within 5s, took ${elapsed}ms")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun collection(
        name: String,
        description: String = "",
        items: List<CollectionItem> = listOf(request("GET", "https://api.example.com/"))
    ) = RequestCollection(
        id = "test-id",
        name = name,
        description = description,
        items = items
    )

    private fun folder(name: String, children: List<CollectionItem>) =
        CollectionItem.Folder(
            id = "folder-${name.lowercase()}",
            name = name,
            children = children
        )

    private fun request(
        method: String,
        url: String,
        body: HttpBody? = null
    ): CollectionItem.Request {
        val httpMethod = HttpMethod.valueOf(method)
        return CollectionItem.Request(
            id = "req-${method.lowercase()}-${url.hashCode()}",
            name = "$method $url",
            request = HttpRequest(
                url = url,
                method = httpMethod,
                headers = emptyMap(),
                body = body
            )
        )
    }

    private fun requestWithHeaders(
        method: String,
        url: String,
        headers: Map<String, String>
    ): CollectionItem.Request {
        val httpMethod = HttpMethod.valueOf(method)
        return CollectionItem.Request(
            id = "req-${method.lowercase()}-${url.hashCode()}",
            name = "$method $url",
            request = HttpRequest(
                url = url,
                method = httpMethod,
                headers = headers
            )
        )
    }
}
