package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.BodyType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Unit tests for CurlExporter */
class CurlExporterTest {

    @Test
    fun testExportSimpleGetRequest() {
        val request = HttpRequest(url = "https://api.example.com/users", method = HttpMethod.GET)

        val curl = CurlExporter.export(request)

        assertTrue(curl.startsWith("curl"))
        assertTrue(curl.contains("'https://api.example.com/users'"))
        assertFalse(curl.contains("-X")) // GET is default, no need for -X
    }

    @Test
    fun testExportPostRequest() {
        val request = HttpRequest(url = "https://api.example.com/users", method = HttpMethod.POST)

        val curl = CurlExporter.export(request)

        assertTrue(curl.contains("-X POST"))
    }

    @Test
    fun testExportRequestWithHeaders() {
        val request =
                HttpRequest(
                        url = "https://api.example.com/users",
                        method = HttpMethod.GET,
                        headers =
                                mapOf(
                                        "Content-Type" to "application/json",
                                        "Authorization" to "Bearer token123"
                                )
                )

        val curl = CurlExporter.export(request)

        assertTrue(curl.contains("-H 'Content-Type: application/json'"))
        assertTrue(curl.contains("-H 'Authorization: Bearer token123'"))
    }

    @Test
    fun testExportRequestWithBody() {
        val request =
                HttpRequest(
                        url = "https://api.example.com/users",
                        method = HttpMethod.POST,
                        body = HttpBody.of("""{"name":"John"}""", BodyType.JSON)
                )

        val curl = CurlExporter.export(request)

        assertTrue(curl.contains("-d '{\"name\":\"John\"}'"))
    }

    @Test
    fun testExportMultilineFormat() {
        val request =
                HttpRequest(
                        url = "https://api.example.com/users",
                        method = HttpMethod.POST,
                        headers = mapOf("Content-Type" to "application/json"),
                        body = HttpBody.of("""{"name":"John"}""", BodyType.JSON)
                )

        val curl = CurlExporter.export(request, multiLine = true)

        assertTrue(curl.contains(" \\\n"))
    }

    @Test
    fun testExportWithOptions() {
        val request = HttpRequest(url = "https://api.example.com/users", method = HttpMethod.GET)

        val curl =
                CurlExporter.exportWithOptions(
                        request,
                        followRedirects = true,
                        verbose = true,
                        includeHeaders = true
                )

        assertTrue(curl.contains("-L"))
        assertTrue(curl.contains("-v"))
        assertTrue(curl.contains("-i"))
    }

    @Test
    fun testExportEscapesSingleQuotes() {
        val request =
                HttpRequest(
                        url = "https://api.example.com/users",
                        method = HttpMethod.POST,
                        body = HttpBody.of("""{"message":"It's a test"}""", BodyType.JSON)
                )

        val curl = CurlExporter.export(request)

        // Single quotes should be escaped
        assertTrue(curl.contains("It'\\''s"))
    }

    @Test
    fun testExportPutRequest() {
        val request =
                HttpRequest(
                        url = "https://api.example.com/users/1",
                        method = HttpMethod.PUT,
                        body = HttpBody.of("""{"id":1}""", BodyType.JSON)
                )

        val curl = CurlExporter.export(request)

        assertTrue(curl.contains("-X PUT"))
    }

    @Test
    fun testExportDeleteRequest() {
        val request =
                HttpRequest(url = "https://api.example.com/users/1", method = HttpMethod.DELETE)

        val curl = CurlExporter.export(request)

        assertTrue(curl.contains("-X DELETE"))
    }

    @Test
    fun testExportPatchRequest() {
        val request =
                HttpRequest(
                        url = "https://api.example.com/users/1",
                        method = HttpMethod.PATCH,
                        body = HttpBody.of("""{"name":"Jane"}""", BodyType.JSON)
                )

        val curl = CurlExporter.export(request)

        assertTrue(curl.contains("-X PATCH"))
    }

    @Test
    fun testRoundTripParseExport() {
        val originalRequest =
                HttpRequest(
                        url = "https://api.example.com/users",
                        method = HttpMethod.POST,
                        headers =
                                mapOf(
                                        "Content-Type" to "application/json",
                                        "X-Custom-Header" to "CustomValue"
                                ),
                        body = HttpBody("""{"name":"John","age":30}""", BodyType.JSON)
                )

        val curlCommand = CurlExporter.export(originalRequest)
        val parsedRequest = CurlParser.parse(curlCommand)

        assertEquals(originalRequest.url, parsedRequest.url)
        assertEquals(originalRequest.method, parsedRequest.method)
        assertEquals(originalRequest.body, parsedRequest.body)
        assertEquals(originalRequest.headers["Content-Type"], parsedRequest.headers["Content-Type"])
    }

    @Test
    fun testExportWithContentType() {
        val request =
                HttpRequest(
                        url = "https://api.example.com/users",
                        method = HttpMethod.POST,
                        headers = emptyMap(),
                        body = HttpBody.of("""{"name":"John"}""", BodyType.JSON)
                )

        val curl = CurlExporter.exportWithOptions(request)

        assertTrue(curl.contains("-H 'Content-Type: application/json'"))
    }

    @Test
    fun testFormatForDisplay() {
        val curlCommand = "curl -X POST -H 'Content-Type: application/json' 'https://example.com'"
        val tokens = CurlExporter.formatForDisplay(curlCommand)

        assertTrue(tokens.isNotEmpty())
        assertTrue(tokens.any { it.second == CurlExporter.TokenType.COMMAND })
        assertTrue(tokens.any { it.second == CurlExporter.TokenType.OPTION })
    }
}
