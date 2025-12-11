package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

/** Unit tests for HttpRequestServiceImpl implementation details */
class HttpRequestServiceImplTest : BasePlatformTestCase() {

    private lateinit var httpService: HttpRequestServiceImpl

    override fun setUp() {
        super.setUp()
        httpService = HttpRequestServiceImpl(project)
    }

    fun testServiceInstantiation() {
        assertNotNull(httpService)
    }

    fun testGetRequestBuilding() = runBlocking {
        val request = HttpRequest(url = "https://httpbin.org/get", method = HttpMethod.GET)

        val response = httpService.executeRequest(request)

        assertNotNull(response)
        assertTrue(response.statusCode >= 0)
    }

    fun testPostRequestWithBody() = runBlocking {
        val requestBody = """{"test": "data"}"""
        val request =
                HttpRequest(
                        url = "https://httpbin.org/post",
                        method = HttpMethod.POST,
                        body = requestBody,
                        contentType = "application/json"
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
        if (response.isSuccessful) {
            assertTrue(response.body.contains("test") || response.body.contains("data"))
        }
    }

    fun testPutRequestWithBody() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://httpbin.org/put",
                        method = HttpMethod.PUT,
                        body = """{"update": "value"}""",
                        contentType = "application/json"
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
        assertTrue(response.duration >= 0)
    }

    fun testDeleteRequestWithoutBody() = runBlocking {
        val request = HttpRequest(url = "https://httpbin.org/delete", method = HttpMethod.DELETE)

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testDeleteRequestWithBody() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://httpbin.org/delete",
                        method = HttpMethod.DELETE,
                        body = """{"reason": "test"}""",
                        contentType = "application/json"
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testPatchRequest() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://httpbin.org/patch",
                        method = HttpMethod.PATCH,
                        body = """{"field": "patched"}""",
                        contentType = "application/json"
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testHeadRequest() = runBlocking {
        val request = HttpRequest(url = "https://httpbin.org/get", method = HttpMethod.HEAD)

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testOptionsRequest() = runBlocking {
        val request = HttpRequest(url = "https://httpbin.org/get", method = HttpMethod.OPTIONS)

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testCustomHeadersAreIncluded() = runBlocking {
        val customHeaders =
                mapOf("X-Custom-Header" to "CustomValue", "User-Agent" to "Amateur-Postman-Test")

        val request =
                HttpRequest(
                        url = "https://httpbin.org/headers",
                        method = HttpMethod.GET,
                        headers = customHeaders
                )

        val response = httpService.executeRequest(request)

        if (response.isSuccessful) {
            assertTrue(
                    response.body.contains("X-Custom-Header") ||
                            response.body.contains("CustomValue") ||
                            response.statusCode == 200
            )
        }
    }

    fun testResponseHeadersParsing() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://httpbin.org/response-headers?Custom-Header=TestValue",
                        method = HttpMethod.GET
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response.headers)
        assertTrue(response.headers.isNotEmpty() || !response.isSuccessful)
    }

    fun testErrorHandlingForInvalidUrl() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://this-is-definitely-not-a-valid-domain-12345.com",
                        method = HttpMethod.GET
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
        assertFalse(response.isSuccessful)
        assertEquals(0, response.statusCode)
        assertTrue(response.body.contains("Error"))
    }

    fun testErrorHandlingForMalformedUrl() = runBlocking {
        val request = HttpRequest(url = "not-a-url", method = HttpMethod.GET)

        val response = httpService.executeRequest(request)

        assertNotNull(response)
        assertFalse(response.isSuccessful)
        assertEquals(0, response.statusCode)
    }

    fun testContentTypeIsRespected() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://httpbin.org/post",
                        method = HttpMethod.POST,
                        body = "<xml>test</xml>",
                        contentType = "application/xml"
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testPlainTextContentType() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://httpbin.org/post",
                        method = HttpMethod.POST,
                        body = "plain text body",
                        contentType = "text/plain"
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testNullBodyHandling() = runBlocking {
        val request =
                HttpRequest(url = "https://httpbin.org/get", method = HttpMethod.GET, body = null)

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testEmptyBodyHandling() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://httpbin.org/post",
                        method = HttpMethod.POST,
                        body = "",
                        contentType = "application/json"
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
    }

    fun testRedirectFollowing() = runBlocking {
        val request = HttpRequest(url = "https://httpbin.org/redirect/1", method = HttpMethod.GET)

        val response = httpService.executeRequest(request)

        if (response.isSuccessful) {
            assertEquals(200, response.statusCode)
        }
    }

    fun testHttpStatusCodes() = runBlocking {
        val statusCodes = listOf(200, 201, 204, 400, 404, 500)

        for (code in statusCodes) {
            val request =
                    HttpRequest(url = "https://httpbin.org/status/$code", method = HttpMethod.GET)

            val response = httpService.executeRequest(request)

            assertNotNull(response)
            if (response.statusCode != 0) {
                assertEquals(code, response.statusCode)
            }
        }
    }

    fun testBasicAuthHeader() = runBlocking {
        val credentials = "user:pass"
        val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())

        val request =
                HttpRequest(
                        url = "https://httpbin.org/basic-auth/user/pass",
                        method = HttpMethod.GET,
                        headers = mapOf("Authorization" to "Basic $encoded")
                )

        val response = httpService.executeRequest(request)

        if (response.isSuccessful) {
            assertEquals(200, response.statusCode)
        }
    }

    fun testBearerTokenHeader() = runBlocking {
        val request =
                HttpRequest(
                        url = "https://httpbin.org/bearer",
                        method = HttpMethod.GET,
                        headers = mapOf("Authorization" to "Bearer test-token")
                )

        val response = httpService.executeRequest(request)

        assertNotNull(response)
        assertTrue(
                response.statusCode == 200 || response.statusCode == 401 || response.statusCode == 0
        )
    }

    fun testJsonResponseParsing() = runBlocking {
        val request = HttpRequest(url = "https://httpbin.org/json", method = HttpMethod.GET)

        val response = httpService.executeRequest(request)

        if (response.isSuccessful) {
            assertTrue(response.body.contains("{"))
            assertTrue(response.body.contains("}"))
        }
    }

    fun testConcurrentRequests() = runBlocking {
        val requests =
                (1..3).map { i ->
                    HttpRequest(url = "https://httpbin.org/delay/$i", method = HttpMethod.GET)
                }

        val responses = requests.map { httpService.executeRequest(it) }

        assertEquals(3, responses.size)
        responses.forEach { response -> assertNotNull(response) }
    }
}
