package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.mockito.kotlin.mock

/**
 * Tests for OAuth2 auto-refresh mechanism.
 */
class OAuth2AutoRefreshTest {

    private lateinit var oauth2Service: OAuth2Service

    @BeforeEach
    fun setUp() {
        oauth2Service = OAuth2Service(mock<Project>())
    }

    // ========== getValidAuth Tests ==========

    @Test
    fun testGetValidAuthWithValidToken(): Unit = runBlocking {
        val validToken = OAuth2Token(
            accessToken = "valid-token",
            expiresIn = 3600 // 1 hour
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            clientSecret = "secret",
            accessToken = validToken
        )

        val entry = oauth2Service.createConfig("Valid Token", config)
        val auth = oauth2Service.getValidAuth(entry.id)

        Assertions.assertNotNull(auth)
        val headers = auth!!.toHeaders()
        Assertions.assertEquals("Bearer valid-token", headers["Authorization"])
    }

    @Test
    fun testGetValidAuthWithNoToken(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            clientSecret = "secret"
        )

        val entry = oauth2Service.createConfig("No Token", config)
        val auth = oauth2Service.getValidAuth(entry.id)

        Assertions.assertNull(auth)
    }

    @Test
    fun testGetValidAuthConfigNotFound(): Unit = runBlocking {
        val auth = oauth2Service.getValidAuth("non-existent")
        Assertions.assertNull(auth)
    }

    @Test
    fun testGetValidAuthExpiredTokenNoRefresh(): Unit = runBlocking {
        val expiredToken = OAuth2Token(
            accessToken = "expired-token",
            expiresIn = 0,
            createdAt = 0, // Very old
            refreshToken = null // No refresh token
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            clientSecret = "secret",
            accessToken = expiredToken
        )

        val entry = oauth2Service.createConfig("Expired No Refresh", config)
        val auth = oauth2Service.getValidAuth(entry.id)

        // Should return null because token is expired and no refresh token
        Assertions.assertNull(auth)
    }

    // ========== ensureValidToken Tests ==========

    @Test
    fun testEnsureValidTokenWithValidToken(): Unit = runBlocking {
        val validToken = OAuth2Token(
            accessToken = "valid-token",
            expiresIn = 3600
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            accessToken = validToken
        )

        val entry = oauth2Service.createConfig("Valid", config)
        val result = oauth2Service.ensureValidToken(entry.id)

        Assertions.assertTrue(result)
    }

    @Test
    fun testEnsureValidTokenNoToken(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            clientSecret = "secret"
        )

        val entry = oauth2Service.createConfig("No Token", config)
        val result = oauth2Service.ensureValidToken(entry.id)

        Assertions.assertFalse(result)
    }

    // ========== Request-Config Mapping with Auto-Refresh Tests ==========

    @Test
    fun testGetValidAuthForRequest(): Unit = runBlocking {
        val validToken = OAuth2Token(
            accessToken = "request-token",
            expiresIn = 3600
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            accessToken = validToken
        )

        val entry = oauth2Service.createConfig("Request Auth", config)
        oauth2Service.setRequestAuthConfig("request-123", entry.id)

        val auth = oauth2Service.getValidAuthForRequest("request-123")

        Assertions.assertNotNull(auth)
        Assertions.assertEquals("Bearer request-token", auth!!.toHeaders()["Authorization"])
    }

    @Test
    fun testGetValidAuthForRequestNoMapping(): Unit = runBlocking {
        val auth = oauth2Service.getValidAuthForRequest("unknown-request")
        Assertions.assertNull(auth)
    }

    @Test
    fun testGetValidAuthForRequestExpiredToken(): Unit = runBlocking {
        val expiredToken = OAuth2Token(
            accessToken = "expired-request-token",
            expiresIn = 0,
            createdAt = 0,
            refreshToken = null
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            accessToken = expiredToken
        )

        val entry = oauth2Service.createConfig("Expired Request", config)
        oauth2Service.setRequestAuthConfig("request-expired", entry.id)

        val auth = oauth2Service.getValidAuthForRequest("request-expired")

        // Should return null because token is expired and no refresh token
        Assertions.assertNull(auth)
    }

    // ========== Collection Auth with Auto-Refresh Tests ==========

    @Test
    fun testGetValidAuthForCollection(): Unit = runBlocking {
        val validToken = OAuth2Token(
            accessToken = "collection-token",
            expiresIn = 7200
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            clientSecret = "secret",
            accessToken = validToken
        )

        val entry = oauth2Service.createConfig("Collection Auth", config)
        oauth2Service.setCollectionAuthConfig("collection-123", entry.id)

        val auth = oauth2Service.getValidAuthForCollection("collection-123")

        Assertions.assertNotNull(auth)
        Assertions.assertEquals("Bearer collection-token", auth!!.toHeaders()["Authorization"])
    }

    @Test
    fun testGetValidAuthForCollectionNoMapping(): Unit = runBlocking {
        val auth = oauth2Service.getValidAuthForCollection("unknown-collection")
        Assertions.assertNull(auth)
    }

    // ========== Token Expiry Edge Cases ==========

    @Test
    fun testTokenExpiryBuffer(): Unit = runBlocking {
        // Token that expires in 30 seconds (within 60 second buffer)
        val almostExpiredToken = OAuth2Token(
            accessToken = "almost-expired",
            expiresIn = 30,
            createdAt = System.currentTimeMillis() / 1000 - 1 // Created 1 second ago
        )

        // Check that it's considered expired due to buffer
        Assertions.assertTrue(almostExpiredToken.isExpired())
    }

    @Test
    fun testTokenNotExpiredWithBuffer(): Unit = runBlocking {
        // Token that expires in 120 seconds (outside 60 second buffer)
        val notExpiredToken = OAuth2Token(
            accessToken = "not-expired",
            expiresIn = 120,
            createdAt = System.currentTimeMillis() / 1000
        )

        Assertions.assertFalse(notExpiredToken.isExpired())
    }

    @Test
    fun testTokenCanRefresh(): Unit = runBlocking {
        val tokenWithRefresh = OAuth2Token(
            accessToken = "token",
            refreshToken = "refresh-token"
        )
        Assertions.assertTrue(tokenWithRefresh.canRefresh())

        val tokenWithoutRefresh = OAuth2Token(
            accessToken = "token"
        )
        Assertions.assertFalse(tokenWithoutRefresh.canRefresh())

        val tokenWithBlankRefresh = OAuth2Token(
            accessToken = "token",
            refreshToken = ""
        )
        Assertions.assertFalse(tokenWithBlankRefresh.canRefresh())
    }

    // ========== OAuth2Auth Auto-Refresh Integration ==========

    @Test
    fun testOAuth2AuthWithValidTokenReturnsHeaders(): Unit = runBlocking {
        val validToken = OAuth2Token(
            accessToken = "auth-token",
            expiresIn = 3600
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client"
        )

        val auth = OAuth2Auth(config, validToken)
        val headers = auth.toHeaders()

        Assertions.assertEquals("Bearer auth-token", headers["Authorization"])
    }

    @Test
    fun testOAuth2AuthWithExpiredTokenReturnsEmptyHeaders(): Unit = runBlocking {
        val expiredToken = OAuth2Token(
            accessToken = "expired-auth-token",
            expiresIn = 0,
            createdAt = 0
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client"
        )

        val auth = OAuth2Auth(config, expiredToken)
        val headers = auth.toHeaders()

        Assertions.assertTrue(headers.isEmpty())
    }

    @Test
    fun testOAuth2AuthWithNullTokenReturnsEmptyHeaders(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client"
        )

        val auth = OAuth2Auth(config, null)
        val headers = auth.toHeaders()

        Assertions.assertTrue(headers.isEmpty())
    }

    // ========== HttpRequest Auto-Refresh Integration ==========

    @Test
    fun testHttpRequestWithOAuth2Auth(): Unit = runBlocking {
        val validToken = OAuth2Token(
            accessToken = "http-request-token",
            expiresIn = 3600
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client"
        )

        val request = HttpRequest(
            url = "https://api.example.com/data",
            method = HttpMethod.GET,
            authentication = OAuth2Auth(config, validToken)
        )

        val effectiveHeaders = request.getEffectiveHeaders()
        Assertions.assertEquals("Bearer http-request-token", effectiveHeaders["Authorization"])
    }

    @Test
    fun testHttpRequestWithExpiredOAuth2Auth(): Unit = runBlocking {
        val expiredToken = OAuth2Token(
            accessToken = "expired-http-token",
            expiresIn = 0,
            createdAt = 0
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client"
        )

        val request = HttpRequest(
            url = "https://api.example.com/data",
            method = HttpMethod.GET,
            authentication = OAuth2Auth(config, expiredToken)
        )

        // Expired token should not produce auth headers
        val effectiveHeaders = request.getEffectiveHeaders()
        Assertions.assertTrue(effectiveHeaders.isEmpty())
    }
}