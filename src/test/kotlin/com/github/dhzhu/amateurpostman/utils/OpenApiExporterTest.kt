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
        assertTrue(result.content!!.contains("\"My API\""), "content: ${result.content}")
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
        assertTrue(result.content!!.contains("\"MyAPI\""), "content: ${result.content}")
    }

    @Test
    fun `single folder becomes tag`() {
        val col = collection("API", items = listOf(
            folder("users", listOf(request("GET", "https://api.example.com/users")))
        ))
        val result = OpenApiExporter.exportCollection(col, OpenApiExporter.ExportFormat.JSON)

        assertTrue(result.isSuccess)
        assertTrue(result.content!!.contains("\"users\""), "content: ${result.content}")
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
        val tagOccurrences = Regex("\"name\"\\s*:\\s*\"users\"").findAll(content).count()
        assertEquals(1, tagOccurrences, "Tag 'users' should appear exactly once in tags array")
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
}
