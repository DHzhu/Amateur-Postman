package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.BodyType
import com.github.dhzhu.amateurpostman.models.CollectionItem
import com.github.dhzhu.amateurpostman.models.HttpMethod
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Tests for OpenApiParser and OpenApiImporter.
 */
class OpenApiImporterTest {

    @Nested
    @DisplayName("OpenApiParser Tests")
    inner class ParserTests {

        @Test
        @DisplayName("Parse valid OpenAPI 3.0 JSON content")
        fun parseValidOpenApi3Json() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Test API",
                    "version": "1.0.0"
                  },
                  "paths": {}
                }
            """.trimIndent()

            val result = OpenApiParser.parseFromContent(jsonContent)

            assertTrue(result.isSuccess)
            assertNotNull(result.openAPI)
            assertEquals("Test API", result.openAPI?.info?.title)
            assertEquals("1.0.0", result.openAPI?.info?.version)
        }

        @Test
        @DisplayName("Parse valid OpenAPI 3.0 YAML content")
        fun parseValidOpenApi3Yaml() {
            val yamlContent = """
                openapi: 3.0.0
                info:
                  title: Test API YAML
                  version: 1.0.0
                paths: {}
            """.trimIndent()

            val result = OpenApiParser.parseFromContent(yamlContent)

            assertTrue(result.isSuccess)
            assertNotNull(result.openAPI)
            assertEquals("Test API YAML", result.openAPI?.info?.title)
        }

        @Test
        @DisplayName("Parse invalid content returns error")
        fun parseInvalidContent() {
            val invalidContent = "This is not a valid OpenAPI document"

            val result = OpenApiParser.parseFromContent(invalidContent)

            assertFalse(result.isSuccess)
            assertNotNull(result.error)
        }

        @Test
        @DisplayName("Validate valid OpenAPI content")
        fun validateValidContent() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Valid API",
                    "version": "1.0.0"
                  },
                  "paths": {}
                }
            """.trimIndent()

            assertTrue(OpenApiParser.isValidOpenApi(jsonContent))
        }

        @Test
        @DisplayName("Validate invalid OpenAPI content")
        fun validateInvalidContent() {
            assertFalse(OpenApiParser.isValidOpenApi("not valid json"))
        }
    }

    @Nested
    @DisplayName("OpenApiImporter Tests")
    inner class ImporterTests {

        @Test
        @DisplayName("Import simple OpenAPI with one GET endpoint")
        fun importSimpleOpenApi() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Simple API",
                    "description": "A simple test API",
                    "version": "1.0.0"
                  },
                  "servers": [
                    {
                      "url": "https://api.example.com",
                      "description": "Production server"
                    }
                  ],
                  "paths": {
                    "/users": {
                      "get": {
                        "summary": "List all users",
                        "description": "Returns a list of users",
                        "tags": ["Users"],
                        "responses": {
                          "200": {
                            "description": "Success"
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val parseResult = OpenApiParser.parseFromContent(jsonContent)
            assertTrue(parseResult.isSuccess)

            val importResult = OpenApiImporter.importFromParseResult(parseResult)

            assertTrue(importResult.isSuccess)
            assertNotNull(importResult.collection)

            val collection = importResult.collection!!
            assertEquals("Simple API", collection.name)
            assertTrue(collection.description.contains("A simple test API"))
            assertTrue(collection.description.contains("Version: 1.0.0"))

            // Check baseUrl variable
            assertTrue(collection.variables.any { it.key == "baseUrl" && it.value == "https://api.example.com" })

            // Check folder structure
            assertEquals(1, collection.items.size)
            val folder = collection.items[0] as? CollectionItem.Folder
            assertNotNull(folder)
            assertEquals("Users", folder?.name)
            assertEquals(1, folder?.children?.size)

            // Check request
            val request = folder?.children?.get(0) as? CollectionItem.Request
            assertNotNull(request)
            assertEquals("List all users", request?.name)
            assertEquals(HttpMethod.GET, request?.request?.method)
            assertEquals("https://api.example.com/users", request?.request?.url)
        }

        @Test
        @DisplayName("Import OpenAPI with multiple HTTP methods")
        fun importMultipleMethods() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Multi-Method API",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/items": {
                      "get": {
                        "summary": "List items",
                        "tags": ["Items"]
                      },
                      "post": {
                        "summary": "Create item",
                        "tags": ["Items"]
                      },
                      "put": {
                        "summary": "Update item",
                        "tags": ["Items"]
                      },
                      "delete": {
                        "summary": "Delete item",
                        "tags": ["Items"]
                      }
                    }
                  }
                }
            """.trimIndent()

            val parseResult = OpenApiParser.parseFromContent(jsonContent)
            val importResult = OpenApiImporter.importFromParseResult(parseResult)

            assertTrue(importResult.isSuccess)
            val collection = importResult.collection!!

            val folder = collection.items[0] as? CollectionItem.Folder
            assertNotNull(folder)
            assertEquals(4, folder?.children?.size)

            val methods = folder?.children?.map {
                (it as CollectionItem.Request).request.method
            }
            assertNotNull(methods)
            assertTrue(methods!!.containsAll(listOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)))
        }

        @Test
        @DisplayName("Import OpenAPI with query and path parameters")
        fun importWithParameters() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Params API",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/users/{id}": {
                      "get": {
                        "summary": "Get user by ID",
                        "tags": ["Users"],
                        "parameters": [
                          {
                            "name": "id",
                            "in": "path",
                            "required": true,
                            "schema": { "type": "integer" },
                            "example": "123"
                          },
                          {
                            "name": "includeDetails",
                            "in": "query",
                            "schema": { "type": "boolean" },
                            "example": "true"
                          }
                        ]
                      }
                    }
                  }
                }
            """.trimIndent()

            val parseResult = OpenApiParser.parseFromContent(jsonContent)
            val importResult = OpenApiImporter.importFromParseResult(parseResult)

            assertTrue(importResult.isSuccess)
            val collection = importResult.collection!!

            val folder = collection.items[0] as? CollectionItem.Folder
            val request = folder?.children?.get(0) as? CollectionItem.Request

            assertNotNull(request)
            // Path parameter should be replaced
            assertTrue(request?.request?.url?.contains("123") == true)
            // Query parameter should be appended
            assertTrue(request?.request?.url?.contains("includeDetails=true") == true)
        }

        @Test
        @DisplayName("Import OpenAPI with request body")
        fun importWithRequestBody() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Body API",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/users": {
                      "post": {
                        "summary": "Create user",
                        "tags": ["Users"],
                        "requestBody": {
                          "content": {
                            "application/json": {
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "name": { "type": "string", "example": "John" },
                                  "email": { "type": "string", "example": "john@example.com" },
                                  "age": { "type": "integer", "example": 30 }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val parseResult = OpenApiParser.parseFromContent(jsonContent)
            val importResult = OpenApiImporter.importFromParseResult(parseResult)

            assertTrue(importResult.isSuccess)
            val collection = importResult.collection!!

            val folder = collection.items[0] as? CollectionItem.Folder
            val request = folder?.children?.get(0) as? CollectionItem.Request

            assertNotNull(request)
            assertNotNull(request?.request?.body)
            assertEquals(BodyType.JSON, request?.request?.body?.type)
            assertTrue(request?.request?.body?.content?.contains("name") == true)
            assertTrue(request?.request?.body?.content?.contains("email") == true)
        }

        @Test
        @DisplayName("Import OpenAPI with header parameters")
        fun importWithHeaderParameters() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Headers API",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/secure": {
                      "get": {
                        "summary": "Secure endpoint",
                        "tags": ["Secure"],
                        "parameters": [
                          {
                            "name": "X-API-Key",
                            "in": "header",
                            "schema": { "type": "string" },
                            "example": "my-api-key"
                          }
                        ]
                      }
                    }
                  }
                }
            """.trimIndent()

            val parseResult = OpenApiParser.parseFromContent(jsonContent)
            val importResult = OpenApiImporter.importFromParseResult(parseResult)

            assertTrue(importResult.isSuccess)
            val collection = importResult.collection!!

            val folder = collection.items[0] as? CollectionItem.Folder
            val request = folder?.children?.get(0) as? CollectionItem.Request

            assertNotNull(request)
            assertEquals("my-api-key", request?.request?.headers?.get("X-API-Key"))
        }

        @Test
        @DisplayName("Import OpenAPI with multiple tags")
        fun importWithMultipleTags() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Multi-Tag API",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/users": {
                      "get": {
                        "summary": "List users",
                        "tags": ["User Management"]
                      }
                    },
                    "/orders": {
                      "get": {
                        "summary": "List orders",
                        "tags": ["Order Management"]
                      }
                    }
                  }
                }
            """.trimIndent()

            val parseResult = OpenApiParser.parseFromContent(jsonContent)
            val importResult = OpenApiImporter.importFromParseResult(parseResult)

            assertTrue(importResult.isSuccess)
            val collection = importResult.collection!!

            assertEquals(2, collection.items.size)
            val folderNames = collection.items.map {
                (it as CollectionItem.Folder).name
            }
            assertTrue(folderNames.contains("User Management"))
            assertTrue(folderNames.contains("Order Management"))
        }

        @Test
        @DisplayName("Import OpenAPI with operations without tags (Default folder)")
        fun importWithoutTags() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "No Tags API",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/health": {
                      "get": {
                        "summary": "Health check"
                      }
                    }
                  }
                }
            """.trimIndent()

            val parseResult = OpenApiParser.parseFromContent(jsonContent)
            val importResult = OpenApiImporter.importFromParseResult(parseResult)

            assertTrue(importResult.isSuccess)
            val collection = importResult.collection!!

            assertEquals(1, collection.items.size)
            val folder = collection.items[0] as? CollectionItem.Folder
            assertNotNull(folder)
            assertEquals("Default", folder?.name)
        }

        @Test
        @DisplayName("Import OpenAPI with security schemes extracts variables")
        fun importWithSecuritySchemes() {
            val jsonContent = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Secure API",
                    "version": "1.0.0"
                  },
                  "components": {
                    "securitySchemes": {
                      "ApiKeyAuth": {
                        "type": "apiKey",
                        "in": "header",
                        "name": "X-API-Key"
                      },
                      "BearerAuth": {
                        "type": "http",
                        "scheme": "bearer"
                      }
                    }
                  },
                  "paths": {}
                }
            """.trimIndent()

            val parseResult = OpenApiParser.parseFromContent(jsonContent)
            val importResult = OpenApiImporter.importFromParseResult(parseResult)

            assertTrue(importResult.isSuccess)
            val collection = importResult.collection!!

            val varKeys = collection.variables.map { it.key }
            assertTrue(varKeys.any { it.contains("api") || it.contains("key") || it == "bearer_token" })
        }
    }
}