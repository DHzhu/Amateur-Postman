package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

/** Integration tests for HttpRequestService */
class HttpRequestServiceTest : BasePlatformTestCase() {

        private lateinit var httpService: HttpRequestService

        override fun setUp() {
                super.setUp()
                httpService = project.service<HttpRequestService>()
        }

        fun testServiceIsAvailable() {
                assertNotNull(httpService)
        }

        fun testGetRequest() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.GET
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(200, response.statusCode)
                assertTrue(response.body.isNotEmpty())
                assertTrue(response.duration > 0)
        }

        fun testGetRequestWithQueryParams() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts?userId=1",
                                method = HttpMethod.GET
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(200, response.statusCode)
                assertTrue(response.body.contains("userId"))
        }

        fun testPostRequest() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts",
                                method = HttpMethod.POST,
                                headers = mapOf("Content-Type" to "application/json"),
                                body =
                                        """{"title": "Test Post", "body": "Test Body", "userId": 1}""",
                                contentType = "application/json"
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(201, response.statusCode)
                assertTrue(response.body.contains("Test Post"))
        }

        fun testPutRequest() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.PUT,
                                headers = mapOf("Content-Type" to "application/json"),
                                body =
                                        """{"id": 1, "title": "Updated Title", "body": "Updated Body", "userId": 1}""",
                                contentType = "application/json"
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(200, response.statusCode)
                assertTrue(response.body.contains("Updated Title"))
        }

        fun testPatchRequest() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.PATCH,
                                headers = mapOf("Content-Type" to "application/json"),
                                body = """{"title": "Patched Title"}""",
                                contentType = "application/json"
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(200, response.statusCode)
        }

        fun testDeleteRequest() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.DELETE
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(200, response.statusCode)
        }

        fun testHeadRequest() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.HEAD
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(200, response.statusCode)
        }

        fun testOptionsRequest() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts",
                                method = HttpMethod.OPTIONS
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.statusCode in 200..299 || response.statusCode == 204)
        }

        fun testInvalidUrl() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://this-domain-does-not-exist-12345.com",
                                method = HttpMethod.GET
                        )

                val response = httpService.executeRequest(request)

                assertFalse(response.isSuccessful)
                assertEquals(0, response.statusCode)
                assertTrue(response.body.contains("Error"))
        }

        fun testNotFoundResponse() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/999999",
                                method = HttpMethod.GET
                        )

                val response = httpService.executeRequest(request)

                assertEquals(404, response.statusCode)
                assertFalse(response.isSuccessful)
        }

        fun testCustomHeaders() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.GET,
                                headers =
                                        mapOf(
                                                "User-Agent" to "Amateur-Postman-Test",
                                                "Accept" to "application/json"
                                        )
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(200, response.statusCode)
        }

        fun testResponseHeaders() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.GET
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertFalse(response.headers.isEmpty())
        }

        fun testJsonResponse() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.GET
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertTrue(response.body.contains("{"))
                assertTrue(response.body.contains("}"))
        }

        fun testEmptyBodyRequest() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.GET,
                                body = null
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertEquals(200, response.statusCode)
        }

        fun testResponseDuration() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.GET
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.duration > 0)
                assertTrue(response.duration < 30000)
        }

        fun testMultipleSequentialRequests() = runBlocking {
                val request1 =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.GET
                        )

                val request2 =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/2",
                                method = HttpMethod.GET
                        )

                val response1 = httpService.executeRequest(request1)
                val response2 = httpService.executeRequest(request2)

                assertTrue(response1.isSuccessful)
                assertTrue(response2.isSuccessful)
                assertNotSame(response1.body, response2.body)
        }

        fun testStatusMessage() = runBlocking {
                val request =
                        HttpRequest(
                                url = "https://jsonplaceholder.typicode.com/posts/1",
                                method = HttpMethod.GET
                        )

                val response = httpService.executeRequest(request)

                assertTrue(response.isSuccessful)
                assertNotNull(response.statusMessage)
                assertTrue(response.statusMessage.isNotEmpty())
        }
}
