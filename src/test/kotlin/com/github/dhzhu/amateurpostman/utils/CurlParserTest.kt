package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.BodyType
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for CurlParser */
class CurlParserTest {

    @Test
    fun testParseSimpleGetRequest() {
        val curl = "curl https://api.example.com/users"
        val request = CurlParser.parse(curl)

        assertEquals("https://api.example.com/users", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertTrue(request.headers.isEmpty())
        assertNull(request.body)
    }

    @Test
    fun testParseGetRequestWithQuotedUrl() {
        val curl = "curl 'https://api.example.com/users?name=John Doe'"
        val request = CurlParser.parse(curl)

        assertEquals("https://api.example.com/users?name=John Doe", request.url)
        assertEquals(HttpMethod.GET, request.method)
    }

    @Test
    fun testParsePostRequestWithMethod() {
        val curl = "curl -X POST https://api.example.com/users"
        val request = CurlParser.parse(curl)

        assertEquals("https://api.example.com/users", request.url)
        assertEquals(HttpMethod.POST, request.method)
    }

    @Test
    fun testParseRequestWithHeader() {
        val curl = "curl -H 'Content-Type: application/json' https://api.example.com/users"
        val request = CurlParser.parse(curl)

        assertEquals("https://api.example.com/users", request.url)
        assertEquals("application/json", request.headers["Content-Type"])
    }

    @Test
    fun testParseRequestWithMultipleHeaders() {
        val curl =
                """curl -H 'Content-Type: application/json' -H 'Authorization: Bearer token123' https://api.example.com/users"""
        val request = CurlParser.parse(curl)

        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("Bearer token123", request.headers["Authorization"])
    }

    @Test
    fun testParsePostRequestWithData() {
        val curl = """curl -X POST -d '{"name":"John"}' https://api.example.com/users"""
        val request = CurlParser.parse(curl)

        assertEquals(HttpMethod.POST, request.method)
        assertEquals("""{"name":"John"}""", request.body?.content)
        assertEquals(BodyType.JSON, request.body?.type)
    }

    @Test
    fun testParsePostRequestWithDataImpliesPostMethod() {
        val curl = """curl -d '{"name":"John"}' https://api.example.com/users"""
        val request = CurlParser.parse(curl)

        // -d should imply POST method
        assertEquals(HttpMethod.POST, request.method)
        assertEquals("""{"name":"John"}""", request.body?.content)
        assertEquals(BodyType.JSON, request.body?.type)
    }

    @Test
    fun testParseRequestWithBasicAuth() {
        val curl = "curl -u username:password https://api.example.com/users"
        val request = CurlParser.parse(curl)

        val expectedAuth =
                "Basic " +
                        java.util.Base64.getEncoder()
                                .encodeToString("username:password".toByteArray())
        assertEquals(expectedAuth, request.headers["Authorization"])
    }

    @Test
    fun testParseRequestWithUserAgent() {
        val curl = "curl -A 'MyAgent/1.0' https://api.example.com/users"
        val request = CurlParser.parse(curl)

        assertEquals("MyAgent/1.0", request.headers["User-Agent"])
    }

    @Test
    fun testParsePutRequest() {
        val curl = "curl -X PUT -d '{\"id\":1}' https://api.example.com/users/1"
        val request = CurlParser.parse(curl)

        assertEquals(HttpMethod.PUT, request.method)
        assertEquals("{\"id\":1}", request.body?.content)
        assertEquals(BodyType.JSON, request.body?.type)
    }

    @Test
    fun testParseDeleteRequest() {
        val curl = "curl -X DELETE https://api.example.com/users/1"
        val request = CurlParser.parse(curl)

        assertEquals(HttpMethod.DELETE, request.method)
    }

    @Test
    fun testParsePatchRequest() {
        val curl = "curl -X PATCH -d '{\"name\":\"Jane\"}' https://api.example.com/users/1"
        val request = CurlParser.parse(curl)

        assertEquals(HttpMethod.PATCH, request.method)
    }

    @Test
    fun testParseMultilineCurlCommand() {
        val curl =
                """
            curl \
              -X POST \
              -H 'Content-Type: application/json' \
              -d '{"name":"John"}' \
              https://api.example.com/users
        """.trimIndent()
        val request = CurlParser.parse(curl)

        assertEquals(HttpMethod.POST, request.method)
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("""{"name":"John"}""", request.body?.content)
        assertEquals(BodyType.JSON, request.body?.type)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseInvalidCommandThrows() {
        CurlParser.parse("wget https://example.com")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseNoUrlThrows() {
        CurlParser.parse("curl -X POST")
    }

    @Test
    fun testParseIgnoresUnknownOptions() {
        val curl = "curl --compressed -k -L https://api.example.com/users"
        val request = CurlParser.parse(curl)

        assertEquals("https://api.example.com/users", request.url)
        assertEquals(HttpMethod.GET, request.method)
    }

    @Test
    fun testParseWithDoubleQuotes() {
        val curl = """curl -H "Content-Type: application/json" "https://api.example.com/users""""
        val request = CurlParser.parse(curl)

        assertEquals("https://api.example.com/users", request.url)
        assertEquals("application/json", request.headers["Content-Type"])
    }

    @Test
    fun testParseContentTypeFromHeader() {
        val curl = "curl -H 'Content-Type: text/xml' https://api.example.com/soap"
        val request = CurlParser.parse(curl)

        assertEquals("text/xml", request.headers["Content-Type"])
    }
}
