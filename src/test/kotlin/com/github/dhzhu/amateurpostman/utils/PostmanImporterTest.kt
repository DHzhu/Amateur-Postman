package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.CollectionItem
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.BodyType
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PostmanImporterTest {

    @Test
    fun `import simple collection with one request`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection",
                    "description": "A test collection"
                },
                "item": [
                    {
                        "name": "Get Users",
                        "request": {
                            "method": "GET",
                            "header": [],
                            "url": {
                                "raw": "https://api.example.com/users",
                                "protocol": "https",
                                "host": ["api", "example", "com"],
                                "path": ["users"]
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        assertNotNull(result.collection)
        assertEquals("Test Collection", result.collection?.name)
        assertEquals("A test collection", result.collection?.description)
        assertEquals(1, result.collection?.items?.size)

        val request = result.collection?.items?.first() as? CollectionItem.Request
        assertNotNull(request)
        assertEquals("Get Users", request?.name)
        assertEquals(HttpMethod.GET, request?.request?.method)
        assertEquals("https://api.example.com/users", request?.request?.url)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `import collection with folder structure`() {
        val json = """
            {
                "info": {
                    "name": "API Collection"
                },
                "item": [
                    {
                        "name": "Users",
                        "item": [
                            {
                                "name": "Get Users",
                                "request": {
                                    "method": "GET",
                                    "url": {
                                        "raw": "https://api.example.com/users"
                                    }
                                }
                            },
                            {
                                "name": "Create User",
                                "request": {
                                    "method": "POST",
                                    "url": {
                                        "raw": "https://api.example.com/users"
                                    },
                                    "body": {
                                        "mode": "raw",
                                        "raw": "{\"name\":\"John\"}"
                                    }
                                }
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        val folder = result.collection?.items?.first() as? CollectionItem.Folder
        assertNotNull(folder)
        assertEquals("Users", folder?.name)
        assertEquals(2, folder?.children?.size)

        val firstRequest = folder?.children?.first() as? CollectionItem.Request
        assertEquals("Get Users", firstRequest?.name)
        assertEquals(HttpMethod.GET, firstRequest?.request?.method)

        val secondRequest = folder?.children?.get(1) as? CollectionItem.Request
        assertEquals("Create User", secondRequest?.name)
        assertEquals(HttpMethod.POST, secondRequest?.request?.method)
        assertEquals("{\"name\":\"John\"}", secondRequest?.request?.body?.content)
    }

    @Test
    fun `import collection with headers`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection"
                },
                "item": [
                    {
                        "name": "Request with Headers",
                        "request": {
                            "method": "GET",
                            "header": [
                                {"key": "Authorization", "value": "Bearer token123"},
                                {"key": "Content-Type", "value": "application/json"}
                            ],
                            "url": {
                                "raw": "https://api.example.com/data"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        val request = result.collection?.items?.first() as? CollectionItem.Request
        assertEquals("Bearer token123", request?.request?.headers?.get("Authorization"))
        assertEquals("application/json", request?.request?.headers?.get("Content-Type"))
    }

    @Test
    fun `import collection with query parameters`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection"
                },
                "item": [
                    {
                        "name": "Search",
                        "request": {
                            "method": "GET",
                            "url": {
                                "raw": "https://api.example.com/search?q=test&page=1",
                                "protocol": "https",
                                "host": ["api", "example", "com"],
                                "path": ["search"],
                                "query": [
                                    {"key": "q", "value": "test"},
                                    {"key": "page", "value": "1"}
                                ]
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        val request = result.collection?.items?.first() as? CollectionItem.Request
        assertTrue(request?.request?.url?.contains("q=test") ?: false)
        assertTrue(request?.request?.url?.contains("page=1") ?: false)
    }

    @Test
    fun `import collection with formdata body`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection"
                },
                "item": [
                    {
                        "name": "Submit Form",
                        "request": {
                            "method": "POST",
                            "url": {
                                "raw": "https://api.example.com/submit"
                            },
                            "body": {
                                "mode": "formdata",
                                "formdata": [
                                    {"key": "username", "value": "john"},
                                    {"key": "password", "value": "secret"}
                                ]
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        val request = result.collection?.items?.first() as? CollectionItem.Request
        assertNotNull(request?.request?.body)
        assertTrue(request?.request?.body?.content?.contains("username=john") ?: false)
        assertTrue(request?.request?.body?.content?.contains("password=secret") ?: false)
    }

    @Test
    fun `import collection with disabled parameters`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection"
                },
                "item": [
                    {
                        "name": "Request",
                        "request": {
                            "method": "GET",
                            "url": {
                                "raw": "https://api.example.com/data",
                                "query": [
                                    {"key": "enabled", "value": "yes"},
                                    {"key": "disabled", "value": "no", "disabled": true}
                                ]
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        val request = result.collection?.items?.first() as? CollectionItem.Request
        assertTrue(request?.request?.url?.contains("enabled=yes") ?: false)
        assertFalse(request?.request?.url?.contains("disabled=no") ?: false)
    }

    @Test
    fun `import collection with URL variables`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection"
                },
                "item": [
                    {
                        "name": "Request with Variable",
                        "request": {
                            "method": "GET",
                            "url": {
                                "raw": "https://api.example.com/users/{{userId}}",
                                "protocol": "https",
                                "host": ["api", "example", "com"],
                                "path": ["users", "{{userId}}"],
                                "variable": [
                                    {"key": "userId", "value": "12345"}
                                ]
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        val request = result.collection?.items?.first() as? CollectionItem.Request
        assertTrue(request?.request?.url?.contains("12345") ?: false)
    }

    @Test
    fun `handle invalid JSON`() {
        val json = """
            {
                "info": {
                    "name": "Test"
                },
                "item": [
            This is invalid JSON
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertFalse(result.isSuccess)
        assertNotNull(result.error)
        assertTrue(result.error?.contains("Invalid JSON") ?: false)
    }

    @Test
    fun `handle missing collection name`() {
        val json = """
            {
                "info": {
                    "name": ""
                },
                "item": []
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertFalse(result.isSuccess)
        assertNotNull(result.error)
    }

    @Test
    fun `handle unknown HTTP method with warning`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection"
                },
                "item": [
                    {
                        "name": "Invalid Method",
                        "request": {
                            "method": "INVALID",
                            "url": {
                                "raw": "https://api.example.com/test"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("Unknown HTTP method"))

        // Should default to GET
        val request = result.collection?.items?.first() as? CollectionItem.Request
        assertEquals(HttpMethod.GET, request?.request?.method)
    }

    @Test
    fun `handle items with no request or sub-items`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection"
                },
                "item": [
                    {
                        "name": "Empty Item"
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings[0].contains("Skipped item"))
        assertTrue(result.collection?.items?.isEmpty() ?: false)
    }

    @Test
    fun `handle nested folder structure`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection"
                },
                "item": [
                    {
                        "name": "API",
                        "item": [
                            {
                                "name": "v1",
                                "item": [
                                    {
                                        "name": "Users",
                                        "item": [
                                            {
                                                "name": "Get Users",
                                                "request": {
                                                    "method": "GET",
                                                    "url": {"raw": "https://api.example.com/v1/users"}
                                                }
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        val apiFolder = result.collection?.items?.first() as? CollectionItem.Folder
        assertEquals("API", apiFolder?.name)

        val v1Folder = apiFolder?.children?.first() as? CollectionItem.Folder
        assertEquals("v1", v1Folder?.name)

        val usersFolder = v1Folder?.children?.first() as? CollectionItem.Folder
        assertEquals("Users", usersFolder?.name)

        val request = usersFolder?.children?.first() as? CollectionItem.Request
        assertEquals("Get Users", request?.name)
    }

    @Test
    fun `import collection preserves description`() {
        val json = """
            {
                "info": {
                    "name": "Test Collection",
                    "description": "Collection description"
                },
                "item": [
                    {
                        "name": "Folder",
                        "description": "Folder description",
                        "item": [
                            {
                                "name": "Request",
                                "description": "Request description",
                                "request": {
                                    "method": "GET",
                                    "url": {"raw": "https://api.example.com/test"}
                                }
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val result = PostmanImporter.importFromJson(json)

        assertTrue(result.isSuccess)
        assertEquals("Collection description", result.collection?.description)

        val folder = result.collection?.items?.first() as? CollectionItem.Folder
        assertEquals("Folder", folder?.name)
        // Note: Folder doesn't have a description field in the current model

        val request = folder?.children?.first() as? CollectionItem.Request
        assertEquals("Request description", request?.description)
    }
}
