package com.github.dhzhu.amateurpostman.utils

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.dhzhu.amateurpostman.models.*
import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.BodyType
import com.github.dhzhu.amateurpostman.services.JsonService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class PostmanExporterTest {

    @Test
    fun `export simple collection with one request`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Test Collection",
            description = "A test collection",
            items = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Get Users",
                    description = "Fetch all users",
                    preRequestScript = "",
                    testScript = "",
                    request = HttpRequest(
                        method = HttpMethod.GET,
                        url = "https://api.example.com/users",
                        headers = mapOf("Accept" to "application/json")
                    )
                )
            ),
            variables = emptyList()
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)
        assertNotNull(result.json)
        assertTrue(result.warnings.isEmpty())

        // Verify JSON structure
        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        assertEquals("Test Collection", (jsonCollection["info"] as Map<*, *>)["name"])
        assertEquals("A test collection", (jsonCollection["info"] as Map<*, *>)["description"])

        val items = jsonCollection["item"] as List<*>
        assertEquals(1, items.size)

        val item = items[0] as Map<*, *>
        assertEquals("Get Users", item["name"])
    }

    @Test
    fun `export collection with folder structure`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "API Collection",
            items = listOf(
                CollectionItem.Folder(
                    id = "folder-1",
                    name = "Users",
                    children = listOf(
                        CollectionItem.Request(
                            id = "req-1",
                            name = "Get Users",
                            preRequestScript = "",
                            testScript = "",
                            request = HttpRequest(
                                method = HttpMethod.GET,
                                url = "https://api.example.com/users"
                            )
                        ),
                        CollectionItem.Request(
                            id = "req-2",
                            name = "Create User",
                            preRequestScript = "",
                            testScript = "",
                            request = HttpRequest(
                                method = HttpMethod.POST,
                                url = "https://api.example.com/users",
                                body = HttpBody.of("{\"name\":\"John\"}", BodyType.JSON)
                            )
                        )
                    )
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        val items = jsonCollection["item"] as List<*>
        assertEquals(1, items.size)

        val folder = items[0] as Map<*, *>
        assertEquals("Users", folder["name"])
        assertNull(folder["request"]) // Folder shouldn't have request

        val folderItems = folder["item"] as List<*>
        assertEquals(2, folderItems.size)
    }

    @Test
    fun `export request with headers`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Test",
            items = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Request with Headers",
                    preRequestScript = "",
                    testScript = "",
                    request = HttpRequest(
                        method = HttpMethod.GET,
                        url = "https://api.example.com/data",
                        headers = mapOf(
                            "Authorization" to "Bearer token123",
                            "Content-Type" to "application/json"
                        )
                    )
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        val item = (jsonCollection["item"] as List<*>)[0] as Map<*, *>
        val request = item["request"] as Map<*, *>

        val headers = request["header"] as List<*>
        assertEquals(2, headers.size)

        val authHeader = (headers[0] as Map<*, *>)
        assertEquals("Authorization", authHeader["key"])
        assertEquals("Bearer token123", authHeader["value"])
    }

    @Test
    fun `export request with query parameters`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Test",
            items = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Search",
                    preRequestScript = "",
                    testScript = "",
                    request = HttpRequest(
                        method = HttpMethod.GET,
                        url = "https://api.example.com/search?q=test&page=1"
                    )
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        val item = (jsonCollection["item"] as List<*>)[0] as Map<*, *>
        val request = item["request"] as Map<*, *>
        val url = request["url"] as Map<*, *>

        val queryParams = url["query"] as List<*>
        assertNotNull(queryParams)
        assertTrue(queryParams.size >= 2)
    }

    @Test
    fun `export request with JSON body`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Test",
            items = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Create User",
                    preRequestScript = "",
                    testScript = "",
                    request = HttpRequest(
                        method = HttpMethod.POST,
                        url = "https://api.example.com/users",
                        body = HttpBody.of("{\"name\":\"John\",\"email\":\"john@example.com\"}", BodyType.JSON)
                    )
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        val item = (jsonCollection["item"] as List<*>)[0] as Map<*, *>
        val request = item["request"] as Map<*, *>
        val body = request["body"] as Map<*, *>

        assertEquals("raw", body["mode"])
        assertEquals("{\"name\":\"John\",\"email\":\"john@example.com\"}", body["raw"])
    }

    @Test
    fun `export request with form-urlencoded body`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Test",
            items = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Submit Form",
                    preRequestScript = "",
                    testScript = "",
                    request = HttpRequest(
                        method = HttpMethod.POST,
                        url = "https://api.example.com/submit",
                        body = HttpBody.of("username=john&password=secret", BodyType.FORM_URLENCODED)
                    )
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        val item = (jsonCollection["item"] as List<*>)[0] as Map<*, *>
        val request = item["request"] as Map<*, *>
        val body = request["body"] as Map<*, *>

        assertEquals("urlencoded", body["mode"])
        val urlencoded = body["urlencoded"] as List<*>
        assertNotNull(urlencoded)
        assertTrue(urlencoded.size >= 2)
    }

    @Test
    fun `export empty collection`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Empty Collection",
            items = emptyList()
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        // Postman format: null instead of empty list (or key not present)
        val items = jsonCollection["item"]
        assertNull(items)
    }

    @Test
    fun `export collection with empty folder generates warning`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Test",
            items = listOf(
                CollectionItem.Folder(
                    id = "folder-1",
                    name = "Empty Folder",
                    children = emptyList()
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("Skipping empty folder"))
    }

    @Test
    fun `export different HTTP methods`() {
        val methods = listOf(
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.PATCH,
            HttpMethod.HEAD,
            HttpMethod.OPTIONS
        )

        val items = methods.mapIndexed { index, method ->
            CollectionItem.Request(
                id = "req-$index",
                name = "${method.name} Request",
                preRequestScript = "",
                testScript = "",
                request = HttpRequest(
                    method = method,
                    url = "https://api.example.com/test"
                )
            )
        }

        val collection = RequestCollection(
            id = "col-1",
            name = "Methods Test",
            items = items
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        val jsonItems = jsonCollection["item"] as List<*>

        methods.forEachIndexed { index, method ->
            val item = jsonItems[index] as Map<*, *>
            val request = item["request"] as Map<*, *>
            assertEquals(method.name, request["method"])
        }
    }

    @Test
    fun `export to file`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "File Export Test",
            items = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Test Request",
                    preRequestScript = "",
                    testScript = "",
                    request = HttpRequest(
                        method = HttpMethod.GET,
                        url = "https://api.example.com/test"
                    )
                )
            )
        )

        val tempFile = File.createTempFile("postman-export", ".json")
        try {
            val result = PostmanExporter.exportToFile(collection, tempFile)

            assertTrue(result.isSuccess)
            assertTrue(tempFile.exists())
            assertTrue(tempFile.readText().isNotEmpty())

            // Verify the file content is valid JSON
            val json = JsonService.mapper.readValue<Map<String, Any?>>(tempFile.readText())
            assertEquals("File Export Test", (json["info"] as Map<*, *>)["name"])
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `export preserves request description`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Test",
            items = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Described Request",
                    description = "This is a detailed description",
                    preRequestScript = "",
                    testScript = "",
                    request = HttpRequest(
                        method = HttpMethod.GET,
                        url = "https://api.example.com/test"
                    )
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        val item = (jsonCollection["item"] as List<*>)[0] as Map<*, *>

        assertEquals("This is a detailed description", item["description"])
    }

    @Test
    fun `export with nested folder structure`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Nested Test",
            items = listOf(
                CollectionItem.Folder(
                    id = "folder-1",
                    name = "API",
                    children = listOf(
                        CollectionItem.Folder(
                            id = "folder-2",
                            name = "v1",
                            children = listOf(
                                CollectionItem.Request(
                                    id = "req-1",
                                    name = "Get Users",
                                    preRequestScript = "",
                                    testScript = "",
                                    request = HttpRequest(
                                        method = HttpMethod.GET,
                                        url = "https://api.example.com/v1/users"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)

        val jsonCollection = JsonService.mapper.readValue<Map<String, Any?>>(result.json!!)
        val items = jsonCollection["item"] as List<*>
        val apiFolder = items[0] as Map<*, *>
        val v1Folder = (apiFolder["item"] as List<*>)[0] as Map<*, *>
        val request = (v1Folder["item"] as List<*>)[0] as Map<*, *>

        assertEquals("Get Users", request["name"])
    }

    @Test
    fun `export handles URLs without protocol`() {
        val collection = RequestCollection(
            id = "col-1",
            name = "Test",
            items = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "No Protocol",
                    preRequestScript = "",
                    testScript = "",
                    request = HttpRequest(
                        method = HttpMethod.GET,
                        url = "api.example.com/test"
                    )
                )
            )
        )

        val result = PostmanExporter.exportCollection(collection)

        assertTrue(result.isSuccess)
        // Should export without crashing
        assertTrue(result.json?.isNotEmpty() ?: false)
    }
}
