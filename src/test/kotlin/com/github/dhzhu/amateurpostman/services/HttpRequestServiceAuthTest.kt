package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Tests for HttpRequestServiceImpl authentication handling.
 * Verifies that BasicAuth, BearerToken, and ApiKeyAuth are correctly
 * converted into HTTP headers.
 */
class HttpRequestServiceAuthTest {

    private lateinit var mockServerManager: MockServerManager
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    @BeforeEach
    fun setUp() {
        mockServerManager = MockServerManager()
    }

    @AfterEach
    fun tearDown() {
        mockServerManager.stop()
        mockServerManager.clearRules()
    }

    // ========== Basic Auth Tests ==========

    @Test
    fun `test BasicAuth adds correct Authorization header`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/secure", HttpMethod.GET, """{"status":"ok"}""")
        )

        val request = HttpRequest(
            url = "http://localhost:$port/api/secure",
            method = HttpMethod.GET,
            authentication = BasicAuth("testuser", "testpass")
        )

        // Verify the request has correct effective headers
        val effectiveHeaders = request.getEffectiveHeaders()
        assertTrue(effectiveHeaders.containsKey("Authorization"))
        val authHeader = effectiveHeaders["Authorization"]!!
        assertTrue(authHeader.startsWith("Basic "))

        // Decode and verify credentials
        val encodedCredentials = authHeader.removePrefix("Basic ")
        val decoded = String(Base64.getDecoder().decode(encodedCredentials))
        assertEquals("testuser:testpass", decoded)
    }

    @Test
    fun `test BasicAuth with special characters`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET,
            authentication = BasicAuth("user@domain.com", "p@ss:word!")
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        val authHeader = effectiveHeaders["Authorization"]!!
        val encodedCredentials = authHeader.removePrefix("Basic ")
        val decoded = String(Base64.getDecoder().decode(encodedCredentials))
        assertEquals("user@domain.com:p@ss:word!", decoded)
    }

    // ========== Bearer Token Tests ==========

    @Test
    fun `test BearerToken adds correct Authorization header`(): Unit = runBlocking {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature"
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            authentication = BearerToken(token)
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("Bearer $token", effectiveHeaders["Authorization"])
    }

    @Test
    fun `test BearerToken with empty token`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            authentication = BearerToken("")
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("Bearer ", effectiveHeaders["Authorization"])
    }

    // ========== API Key Auth Tests ==========

    @Test
    fun `test ApiKeyAuth adds correct header`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/data",
            method = HttpMethod.GET,
            authentication = ApiKeyAuth("X-API-Key", "abc123secret")
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("abc123secret", effectiveHeaders["X-API-Key"])
        assertFalse(effectiveHeaders.containsKey("Authorization"))
    }

    @Test
    fun `test ApiKeyAuth with custom header name`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/data",
            method = HttpMethod.GET,
            authentication = ApiKeyAuth("X-Custom-Auth-Token", "my-token-123")
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("my-token-123", effectiveHeaders["X-Custom-Auth-Token"])
    }

    @Test
    fun `test ApiKeyAuth QUERY location does not add header`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/data",
            method = HttpMethod.GET,
            authentication = ApiKeyAuth(
                key = "api_key",
                value = "secret123",
                addTo = ApiKeyAuth.ApiKeyLocation.QUERY
            )
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        // Query params are handled separately, not in headers
        assertFalse(effectiveHeaders.containsKey("api_key"))
        assertFalse(effectiveHeaders.containsKey("Authorization"))
    }

    // ========== NoAuth Tests ==========

    @Test
    fun `test NoAuth does not add any headers`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/public",
            method = HttpMethod.GET,
            authentication = NoAuth
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertTrue(effectiveHeaders.isEmpty())
    }

    @Test
    fun `test null authentication behaves like NoAuth`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/public",
            method = HttpMethod.GET,
            authentication = null
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertTrue(effectiveHeaders.isEmpty())
    }

    // ========== Header Merging Tests ==========

    @Test
    fun `test authentication headers merge with existing headers`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/data",
            method = HttpMethod.POST,
            headers = mapOf(
                "Content-Type" to "application/json",
                "X-Request-ID" to "12345"
            ),
            authentication = BearerToken("my-token")
        )

        val effectiveHeaders = request.getEffectiveHeaders()

        assertEquals("application/json", effectiveHeaders["Content-Type"])
        assertEquals("12345", effectiveHeaders["X-Request-ID"])
        assertEquals("Bearer my-token", effectiveHeaders["Authorization"])
        assertEquals(3, effectiveHeaders.size)
    }

    @Test
    fun `test authentication overrides existing Authorization header`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/data",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Basic old-credentials"),
            authentication = BearerToken("new-token")
        )

        val effectiveHeaders = request.getEffectiveHeaders()

        // Auth header from authentication should override the one in headers
        assertEquals("Bearer new-token", effectiveHeaders["Authorization"])
    }

    @Test
    fun `test BasicAuth overrides existing Authorization header`(): Unit = runBlocking {
        val request = HttpRequest(
            url = "https://api.example.com/data",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer old-token"),
            authentication = BasicAuth("user", "pass")
        )

        val effectiveHeaders = request.getEffectiveHeaders()

        val authHeader = effectiveHeaders["Authorization"]!!
        assertTrue(authHeader.startsWith("Basic "))
        assertFalse(authHeader.contains("Bearer"))
    }

    // ========== OAuth2Auth Tests ==========

    @Test
    fun `test OAuth2Auth with valid token adds Authorization header`(): Unit = runBlocking {
        val token = OAuth2Token(
            accessToken = "access-token-123",
            tokenType = "Bearer",
            expiresIn = 3600
        )
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client"
        )
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            authentication = OAuth2Auth(config, token)
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("Bearer access-token-123", effectiveHeaders["Authorization"])
    }

    @Test
    fun `test OAuth2Auth with expired token returns no Authorization header`(): Unit = runBlocking {
        val expiredToken = OAuth2Token(
            accessToken = "expired-token",
            expiresIn = 0,
            createdAt = 0 // Very old timestamp
        )
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client"
        )
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            authentication = OAuth2Auth(config, expiredToken)
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        // Expired token should not produce headers
        assertFalse(effectiveHeaders.containsKey("Authorization"))
    }

    @Test
    fun `test OAuth2Auth with null token returns no Authorization header`(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client"
        )
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            authentication = OAuth2Auth(config, null)
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertFalse(effectiveHeaders.containsKey("Authorization"))
    }

    @Test
    fun `test OAuth2Auth with custom token type`(): Unit = runBlocking {
        val token = OAuth2Token(
            accessToken = "access-token-123",
            tokenType = "MAC",
            expiresIn = 3600
        )
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client"
        )
        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.GET,
            authentication = OAuth2Auth(config, token)
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        assertEquals("MAC access-token-123", effectiveHeaders["Authorization"])
    }

    // ========== Integration Test with Mock Server ==========

    @Test
    fun `test request with BearerToken reaches mock server`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/protected", HttpMethod.GET, """{"authenticated":true}""")
        )

        // Make actual HTTP request with Bearer token
        val request = Request.Builder()
            .url("http://localhost:$port/api/protected")
            .header("Authorization", "Bearer test-token-123")
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals("""{"authenticated":true}""", response.body?.string())
    }
}