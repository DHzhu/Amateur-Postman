package com.github.dhzhu.amateurpostman.models

import org.junit.Assert.*
import org.junit.Test

/** Unit tests for HTTP Models */
class HttpModelsTest {

        @Test
        fun testHttpRequestCreationWithAllParameters() {
                val headers =
                        mapOf(
                                "Content-Type" to "application/json",
                                "Authorization" to "Bearer token"
                        )
                val request =
                        HttpRequest(
                                url = "https://api.example.com/users",
                                method = HttpMethod.POST,
                                headers = headers,
                                body = """{"name": "John Doe"}""",
                                contentType = "application/json"
                        )

                assertEquals("https://api.example.com/users", request.url)
                assertEquals(HttpMethod.POST, request.method)
                assertEquals(headers, request.headers)
                assertEquals("""{"name": "John Doe"}""", request.body)
                assertEquals("application/json", request.contentType)
        }

        @Test
        fun testHttpRequestCreationWithMinimalParameters() {
                val request =
                        HttpRequest(url = "https://api.example.com/users", method = HttpMethod.GET)

                assertEquals("https://api.example.com/users", request.url)
                assertEquals(HttpMethod.GET, request.method)
                assertTrue(request.headers.isEmpty())
                assertNull(request.body)
                assertNull(request.contentType)
        }

        @Test
        fun testHttpRequestWithEmptyHeaders() {
                val request =
                        HttpRequest(
                                url = "https://api.example.com/data",
                                method = HttpMethod.DELETE,
                                headers = emptyMap()
                        )

                assertTrue(request.headers.isEmpty())
        }

        @Test
        fun testHttpResponseCreationWithSuccessfulStatus() {
                val headers = mapOf("Content-Type" to listOf("application/json"))
                val response =
                        HttpResponse(
                                statusCode = 200,
                                statusMessage = "OK",
                                headers = headers,
                                body = """{"success": true}""",
                                duration = 150L
                        )

                assertEquals(200, response.statusCode)
                assertEquals("OK", response.statusMessage)
                assertEquals(headers, response.headers)
                assertEquals("""{"success": true}""", response.body)
                assertEquals(150L, response.duration)
                assertTrue(response.isSuccessful)
        }

        @Test
        fun testHttpResponseWithErrorStatus() {
                val response =
                        HttpResponse(
                                statusCode = 404,
                                statusMessage = "Not Found",
                                headers = emptyMap(),
                                body = """{"error": "Resource not found"}""",
                                duration = 100L
                        )

                assertEquals(404, response.statusCode)
                assertFalse(response.isSuccessful)
        }

        @Test
        fun testHttpResponseIsSuccessfulForVariousStatusCodes() {
                // Test successful status codes (200-299)
                val successResponse =
                        HttpResponse(
                                statusCode = 201,
                                statusMessage = "Created",
                                headers = emptyMap(),
                                body = "",
                                duration = 100L
                        )
                assertTrue(successResponse.isSuccessful)

                // Test client error (400-499)
                val clientErrorResponse =
                        HttpResponse(
                                statusCode = 400,
                                statusMessage = "Bad Request",
                                headers = emptyMap(),
                                body = "",
                                duration = 100L
                        )
                assertFalse(clientErrorResponse.isSuccessful)

                // Test server error (500-599)
                val serverErrorResponse =
                        HttpResponse(
                                statusCode = 500,
                                statusMessage = "Internal Server Error",
                                headers = emptyMap(),
                                body = "",
                                duration = 100L
                        )
                assertFalse(serverErrorResponse.isSuccessful)
        }

        @Test
        fun testHttpResponseWithMultipleHeaderValues() {
                val headers =
                        mapOf(
                                "Set-Cookie" to listOf("cookie1=value1", "cookie2=value2"),
                                "Content-Type" to listOf("application/json")
                        )
                val response =
                        HttpResponse(
                                statusCode = 200,
                                statusMessage = "OK",
                                headers = headers,
                                body = "",
                                duration = 50L
                        )

                assertEquals(2, response.headers["Set-Cookie"]?.size)
                assertEquals("cookie1=value1", response.headers["Set-Cookie"]?.get(0))
        }

        @Test
        fun testHttpMethodEnumValues() {
                assertEquals("GET", HttpMethod.GET.toString())
                assertEquals("POST", HttpMethod.POST.toString())
                assertEquals("PUT", HttpMethod.PUT.toString())
                assertEquals("DELETE", HttpMethod.DELETE.toString())
                assertEquals("PATCH", HttpMethod.PATCH.toString())
                assertEquals("HEAD", HttpMethod.HEAD.toString())
                assertEquals("OPTIONS", HttpMethod.OPTIONS.toString())
        }

        @Test
        fun testHttpMethodEnumCount() {
                val methods = HttpMethod.values()
                assertEquals(7, methods.size)
        }

        @Test
        fun testHttpMethodValueOf() {
                assertEquals(HttpMethod.GET, HttpMethod.valueOf("GET"))
                assertEquals(HttpMethod.POST, HttpMethod.valueOf("POST"))
                assertEquals(HttpMethod.PUT, HttpMethod.valueOf("PUT"))
                assertEquals(HttpMethod.DELETE, HttpMethod.valueOf("DELETE"))
                assertEquals(HttpMethod.PATCH, HttpMethod.valueOf("PATCH"))
                assertEquals(HttpMethod.HEAD, HttpMethod.valueOf("HEAD"))
                assertEquals(HttpMethod.OPTIONS, HttpMethod.valueOf("OPTIONS"))
        }

        @Test
        fun testHttpRequestDataClassCopy() {
                val original =
                        HttpRequest(url = "https://api.example.com/users", method = HttpMethod.GET)

                val modified = original.copy(method = HttpMethod.POST)

                assertEquals("https://api.example.com/users", modified.url)
                assertEquals(HttpMethod.POST, modified.method)
                assertEquals(HttpMethod.GET, original.method)
        }

        @Test
        fun testHttpResponseDataClassCopy() {
                val original =
                        HttpResponse(
                                statusCode = 200,
                                statusMessage = "OK",
                                headers = emptyMap(),
                                body = "original body",
                                duration = 100L
                        )

                val modified = original.copy(body = "modified body")

                assertEquals("modified body", modified.body)
                assertEquals("original body", original.body)
        }

        @Test
        fun testHttpRequestWithSpecialCharactersInUrl() {
                val request =
                        HttpRequest(
                                url = "https://api.example.com/search?q=test%20query&lang=en",
                                method = HttpMethod.GET
                        )

                assertEquals("https://api.example.com/search?q=test%20query&lang=en", request.url)
        }

        @Test
        fun testHttpResponseWithEmptyBody() {
                val response =
                        HttpResponse(
                                statusCode = 204,
                                statusMessage = "No Content",
                                headers = emptyMap(),
                                body = "",
                                duration = 50L
                        )

                assertEquals("", response.body)
                assertTrue(response.isSuccessful)
        }

        @Test
        fun testHttpRequestEqualsAndHashCode() {
                val request1 =
                        HttpRequest(url = "https://api.example.com/users", method = HttpMethod.GET)

                val request2 =
                        HttpRequest(url = "https://api.example.com/users", method = HttpMethod.GET)

                assertEquals(request1, request2)
                assertEquals(request1.hashCode(), request2.hashCode())
        }

        @Test
        fun testHttpResponseEqualsAndHashCode() {
                val response1 =
                        HttpResponse(
                                statusCode = 200,
                                statusMessage = "OK",
                                headers = emptyMap(),
                                body = "test",
                                duration = 100L
                        )

                val response2 =
                        HttpResponse(
                                statusCode = 200,
                                statusMessage = "OK",
                                headers = emptyMap(),
                                body = "test",
                                duration = 100L
                        )

                assertEquals(response1, response2)
                assertEquals(response1.hashCode(), response2.hashCode())
        }
}
