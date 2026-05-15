package com.github.dhzhu.amateurpostman.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Base64

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
                body = HttpBody.of("""{"name": "John Doe"}""", BodyType.JSON)
            )

        assertEquals("https://api.example.com/users", request.url)
        assertEquals(HttpMethod.POST, request.method)
        assertEquals(headers, request.headers)
        assertEquals("""{"name": "John Doe"}""", request.body?.content)
        assertEquals(BodyType.JSON, request.body?.type)
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
        val methods = HttpMethod.entries
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

    @Test
    fun testBodyTypeEnumValues() {
        assertEquals("Text", BodyType.TEXT.displayName)
        assertEquals("JSON", BodyType.JSON.displayName)
        assertEquals("XML", BodyType.XML.displayName)
        assertEquals("HTML", BodyType.HTML.displayName)
        assertEquals("JavaScript", BodyType.JAVASCRIPT.displayName)
    }

    @Test
    fun testBodyTypeFromMimeType() {
        assertEquals(BodyType.JSON, BodyType.fromMimeType("application/json"))
        assertEquals(BodyType.XML, BodyType.fromMimeType("application/xml"))
        assertEquals(BodyType.HTML, BodyType.fromMimeType("text/html"))
        assertEquals(BodyType.JAVASCRIPT, BodyType.fromMimeType("application/javascript"))
        assertEquals(BodyType.TEXT, BodyType.fromMimeType("text/plain"))
        assertEquals(BodyType.JSON, BodyType.fromMimeType("unknown/type")) // Default to JSON
    }

    @Test
    fun testHttpBodyIsEmpty() {
        val emptyBody = HttpBody.of("", BodyType.JSON)
        assertTrue(emptyBody.isEmpty)

        val blankBody = HttpBody.of("   ", BodyType.JSON)
        assertTrue(blankBody.isEmpty)

        val nonEmptyBody = HttpBody.of("""{"key": "value"}""", BodyType.JSON)
        assertFalse(nonEmptyBody.isEmpty)
    }

    // ============================================================
    // Authentication Model Tests
    // ============================================================

    @Test
    fun testNoAuthToHeaders() {
        val auth = NoAuth
        assertTrue(auth.toHeaders().isEmpty())
    }

    @Test
    fun testBasicAuthToHeaders() {
        val auth = BasicAuth(username = "user", password = "pass")
        val headers = auth.toHeaders()

        assertEquals(1, headers.size)
        assertTrue(headers.containsKey("Authorization"))
        assertTrue(headers["Authorization"]!!.startsWith("Basic "))

        // Verify the Base64 encoded credentials
        val encodedCredentials = headers["Authorization"]!!.removePrefix("Basic ")
        val decoded = String(Base64.getDecoder().decode(encodedCredentials))
        assertEquals("user:pass", decoded)
    }

    @Test
    fun testBasicAuthWithSpecialCharacters() {
        val auth = BasicAuth(username = "user@domain", password = "p@ss:word")
        val headers = auth.toHeaders()

        val encodedCredentials = headers["Authorization"]!!.removePrefix("Basic ")
        val decoded = String(Base64.getDecoder().decode(encodedCredentials))
        assertEquals("user@domain:p@ss:word", decoded)
    }

    @Test
    fun testBearerTokenToHeaders() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"
        val auth = BearerToken(token)
        val headers = auth.toHeaders()

        assertEquals(1, headers.size)
        assertEquals("Bearer $token", headers["Authorization"])
    }

    @Test
    fun testApiKeyAuthToHeaders() {
        val auth = ApiKeyAuth(key = "X-API-Key", value = "abc123")
        val headers = auth.toHeaders()

        assertEquals(1, headers.size)
        assertEquals("abc123", headers["X-API-Key"])
    }

    @Test
    fun testApiKeyAuthQueryParams() {
        val auth = ApiKeyAuth(
            key = "api_key",
            value = "abc123",
            addTo = ApiKeyAuth.ApiKeyLocation.QUERY
        )
        val headers = auth.toHeaders()

        // Query params are not added to headers
        assertTrue(headers.isEmpty())
    }

    @Test
    fun testOAuth2TokenNotExpired() {
        val token = OAuth2Token(
            accessToken = "test-token",
            expiresIn = 3600 // 1 hour
        )
        assertFalse(token.isExpired())
    }

    @Test
    fun testOAuth2TokenExpired() {
        val token = OAuth2Token(
            accessToken = "test-token",
            expiresIn = 0,
            createdAt = 0 // Very old
        )
        assertTrue(token.isExpired())
    }

    @Test
    fun testOAuth2TokenCanRefresh() {
        val tokenWithRefresh = OAuth2Token(
            accessToken = "test-token",
            refreshToken = "refresh-token"
        )
        assertTrue(tokenWithRefresh.canRefresh())

        val tokenWithoutRefresh = OAuth2Token(
            accessToken = "test-token"
        )
        assertFalse(tokenWithoutRefresh.canRefresh())
    }

    @Test
    fun testOAuth2AuthWithValidToken() {
        val token = OAuth2Token(
            accessToken = "test-access-token",
            tokenType = "Bearer",
            expiresIn = 3600
        )
        val auth = OAuth2Auth(
            config = OAuth2Config(
                grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
                tokenUrl = "https://auth.example.com/token",
                clientId = "test-client"
            ),
            token = token
        )
        val headers = auth.toHeaders()

        assertEquals(1, headers.size)
        assertEquals("Bearer test-access-token", headers["Authorization"])
    }

    @Test
    fun testOAuth2AuthWithExpiredToken() {
        val token = OAuth2Token(
            accessToken = "test-access-token",
            expiresIn = 0,
            createdAt = 0
        )
        val auth = OAuth2Auth(
            config = OAuth2Config(
                grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
                tokenUrl = "https://auth.example.com/token",
                clientId = "test-client"
            ),
            token = token
        )
        val headers = auth.toHeaders()

        // Expired token should not produce headers
        assertTrue(headers.isEmpty())
    }

    @Test
    fun testOAuth2AuthWithNullToken() {
        val auth = OAuth2Auth(
            config = OAuth2Config(
                grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
                tokenUrl = "https://auth.example.com/token",
                clientId = "test-client"
            ),
            token = null
        )
        val headers = auth.toHeaders()

        assertTrue(headers.isEmpty())
    }

    @Test
    fun testOAuth2ConfigCreation() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            authUrl = "https://auth.example.com/authorize",
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client",
            clientSecret = "test-secret",
            scope = "read write",
            redirectUri = "http://localhost:8080/callback"
        )

        assertEquals(OAuth2GrantType.AUTHORIZATION_CODE, config.grantType)
        assertEquals("https://auth.example.com/authorize", config.authUrl)
        assertEquals("https://auth.example.com/token", config.tokenUrl)
        assertEquals("test-client", config.clientId)
        assertEquals("test-secret", config.clientSecret)
        assertEquals("read write", config.scope)
        assertEquals("http://localhost:8080/callback", config.redirectUri)
    }

    @Test
    fun testOAuth2GrantTypes() {
        assertEquals("Authorization Code", OAuth2GrantType.AUTHORIZATION_CODE.displayName)
        assertEquals("Client Credentials", OAuth2GrantType.CLIENT_CREDENTIALS.displayName)
        assertEquals("Password", OAuth2GrantType.PASSWORD.displayName)
        assertEquals("Implicit", OAuth2GrantType.IMPLICIT.displayName)
    }

    @Test
    fun testHttpRequestWithBasicAuth() {
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            authentication = BasicAuth("user", "pass")
        )

        assertNotNull(request.authentication)
        assertTrue(request.authentication is BasicAuth)

        val effectiveHeaders = request.getEffectiveHeaders()
        assertTrue(effectiveHeaders.containsKey("Authorization"))
        assertTrue(effectiveHeaders["Authorization"]!!.startsWith("Basic "))
    }

    @Test
    fun testHttpRequestWithBearerToken() {
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            authentication = BearerToken("my-token")
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("Bearer my-token", effectiveHeaders["Authorization"])
    }

    @Test
    fun testHttpRequestEffectiveHeadersMergesAuth() {
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            headers = mapOf("Content-Type" to "application/json"),
            authentication = BearerToken("my-token")
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("application/json", effectiveHeaders["Content-Type"])
        assertEquals("Bearer my-token", effectiveHeaders["Authorization"])
        assertEquals(2, effectiveHeaders.size)
    }

    @Test
    fun testHttpRequestAuthOverridesExistingHeader() {
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Basic old-credentials"),
            authentication = BearerToken("new-token")
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("Bearer new-token", effectiveHeaders["Authorization"])
    }

    @Test
    fun testHttpRequestWithoutAuth() {
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET
        )

        assertNull(request.authentication)
        assertEquals(emptyMap<String, String>(), request.getEffectiveHeaders())
    }
}
