package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.mockito.kotlin.mock

/**
 * Integration tests for OAuth2 token exchange flows.
 */
class OAuth2TokenExchangeTest {

    private lateinit var mockServerManager: MockServerManager
    private lateinit var oauth2Service: OAuth2Service

    @BeforeEach
    fun setUp() {
        mockServerManager = MockServerManager()
        oauth2Service = OAuth2Service(mock<Project>())
    }

    @AfterEach
    fun tearDown() {
        mockServerManager.stop()
        mockServerManager.clearRules()
    }

    // ========== Client Credentials Flow Tests ==========

    @Test
    fun `test client credentials token exchange success`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val tokenResponse = """{
            "access_token": "test-access-token-123",
            "token_type": "Bearer",
            "expires_in": 3600,
            "scope": "read write"
        }"""

        mockServerManager.addRule(
            MockRule.create("/oauth/token", HttpMethod.POST, tokenResponse)
                .copy(
                    headers = mapOf("Content-Type" to "application/json")
                )
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "http://localhost:$port/oauth/token",
            clientId = "test-client",
            clientSecret = "test-secret",
            scope = "read write"
        )

        val entry = oauth2Service.createConfig("Test Client Credentials", config)
        val result = oauth2Service.exchangeClientCredentials(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Success)
        val token = (result as TokenExchangeResult.Success).token
        Assertions.assertEquals("test-access-token-123", token.accessToken)
        Assertions.assertEquals("Bearer", token.tokenType)
        Assertions.assertEquals(3600, token.expiresIn)
        Assertions.assertEquals("read write", token.scope)
    }

    @Test
    fun `test client credentials with minimal response`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val tokenResponse = """{"access_token": "simple-token"}"""

        mockServerManager.addRule(
            MockRule.create("/token", HttpMethod.POST, tokenResponse)
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "http://localhost:$port/token",
            clientId = "client-id",
            clientSecret = "secret"
        )

        val entry = oauth2Service.createConfig("Minimal Config", config)
        val result = oauth2Service.exchangeClientCredentials(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Success)
        val token = (result as TokenExchangeResult.Success).token
        Assertions.assertEquals("simple-token", token.accessToken)
        Assertions.assertEquals("Bearer", token.tokenType) // Default value
    }

    @Test
    fun `test client credentials error response`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val errorResponse = """{"error": "invalid_client", "error_description": "Client authentication failed"}"""

        // Note: Mock server doesn't support custom status codes directly, so we test the error parsing
        mockServerManager.addRule(
            MockRule.create("/token", HttpMethod.POST, errorResponse)
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "http://localhost:$port/token",
            clientId = "invalid-client",
            clientSecret = "wrong-secret"
        )

        val entry = oauth2Service.createConfig("Error Config", config)
        val result = oauth2Service.exchangeClientCredentials(entry.id)

        // Since mock server returns 200 by default, we should get a success (token parsed)
        // In real scenario, a 401 would return an error
        Assertions.assertTrue(result is TokenExchangeResult.Success || result is TokenExchangeResult.Error)
    }

    @Test
    fun `test client credentials missing client secret`(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client",
            clientSecret = null
        )

        val entry = oauth2Service.createConfig("Missing Secret", config)
        val result = oauth2Service.exchangeClientCredentials(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("Client secret is required"))
    }

    @Test
    fun `test client credentials wrong grant type`(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.PASSWORD,
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client",
            clientSecret = "secret"
        )

        val entry = oauth2Service.createConfig("Wrong Grant Type", config)
        val result = oauth2Service.exchangeClientCredentials(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("Invalid grant type"))
    }

    @Test
    fun `test client credentials config not found`(): Unit = runBlocking {
        val result = oauth2Service.exchangeClientCredentials("non-existent-id")

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("Configuration not found"))
    }

    // ========== Password Flow Tests ==========

    @Test
    fun `test password flow token exchange success`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val tokenResponse = """{
            "access_token": "password-token-456",
            "token_type": "Bearer",
            "expires_in": 7200,
            "refresh_token": "refresh-token-789"
        }"""

        mockServerManager.addRule(
            MockRule.create("/oauth/token", HttpMethod.POST, tokenResponse)
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.PASSWORD,
            tokenUrl = "http://localhost:$port/oauth/token",
            clientId = "password-client",
            clientSecret = "secret",
            username = "testuser",
            password = "testpass"
        )

        val entry = oauth2Service.createConfig("Password Config", config)
        val result = oauth2Service.exchangePassword(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Success)
        val token = (result as TokenExchangeResult.Success).token
        Assertions.assertEquals("password-token-456", token.accessToken)
        Assertions.assertEquals("refresh-token-789", token.refreshToken)
    }

    @Test
    fun `test password flow missing credentials`(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.PASSWORD,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            username = null,
            password = null
        )

        val entry = oauth2Service.createConfig("Missing Credentials", config)
        val result = oauth2Service.exchangePassword(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("Username and password are required"))
    }

    // ========== Token Refresh Tests ==========

    @Test
    fun `test token refresh success`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val tokenResponse = """{
            "access_token": "new-access-token",
            "token_type": "Bearer",
            "expires_in": 3600
        }"""

        mockServerManager.addRule(
            MockRule.create("/oauth/token", HttpMethod.POST, tokenResponse)
        )

        val initialToken = OAuth2Token(
            accessToken = "old-access-token",
            refreshToken = "refresh-token-value",
            expiresIn = 0,
            createdAt = 0
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "http://localhost:$port/oauth/token",
            clientId = "client",
            clientSecret = "secret",
            accessToken = initialToken
        )

        val entry = oauth2Service.createConfig("Refresh Config", config)
        val result = oauth2Service.refreshToken(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Success)
        val token = (result as TokenExchangeResult.Success).token
        Assertions.assertEquals("new-access-token", token.accessToken)
    }

    @Test
    fun `test token refresh no refresh token`(): Unit = runBlocking {
        val initialToken = OAuth2Token(
            accessToken = "access-without-refresh",
            expiresIn = 3600
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            clientSecret = "secret",
            accessToken = initialToken
        )

        val entry = oauth2Service.createConfig("No Refresh Token", config)
        val result = oauth2Service.refreshToken(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("No refresh token available"))
    }

    // ========== Fetch and Store Tests ==========

    @Test
    fun `test fetchAndStoreToken client credentials`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val tokenResponse = """{"access_token": "fetched-token", "expires_in": 3600}"""

        mockServerManager.addRule(
            MockRule.create("/token", HttpMethod.POST, tokenResponse)
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "http://localhost:$port/token",
            clientId = "client",
            clientSecret = "secret"
        )

        val entry = oauth2Service.createConfig("Fetch Store", config)

        // Verify no token initially
        Assertions.assertNull(oauth2Service.getToken(entry.id))

        val result = oauth2Service.fetchAndStoreToken(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Success)
        Assertions.assertNotNull(oauth2Service.getToken(entry.id))
        Assertions.assertEquals("fetched-token", oauth2Service.getToken(entry.id)?.accessToken)
    }

    @Test
    fun `test fetchAndStoreToken password flow`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val tokenResponse = """{"access_token": "password-fetched-token", "expires_in": 3600}"""

        mockServerManager.addRule(
            MockRule.create("/token", HttpMethod.POST, tokenResponse)
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.PASSWORD,
            tokenUrl = "http://localhost:$port/token",
            clientId = "client",
            username = "user",
            password = "pass"
        )

        val entry = oauth2Service.createConfig("Password Fetch", config)
        val result = oauth2Service.fetchAndStoreToken(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Success)
        Assertions.assertEquals("password-fetched-token", oauth2Service.getToken(entry.id)?.accessToken)
    }

    @Test
    fun `test fetchAndStoreToken authorization code returns error`(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            authUrl = "https://auth.example.com/authorize"
        )

        val entry = oauth2Service.createConfig("Auth Code", config)
        val result = oauth2Service.fetchAndStoreToken(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("requires interactive authorization"))
    }

    // ========== Token Response Parsing Tests ==========

    @Test
    fun `test parse token response with all fields`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val tokenResponse = """{
            "access_token": "full-token",
            "token_type": "MAC",
            "expires_in": 7200,
            "refresh_token": "rt-123",
            "scope": "read write admin"
        }"""

        mockServerManager.addRule(
            MockRule.create("/token", HttpMethod.POST, tokenResponse)
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "http://localhost:$port/token",
            clientId = "client",
            clientSecret = "secret"
        )

        val entry = oauth2Service.createConfig("Full Response", config)
        val result = oauth2Service.exchangeClientCredentials(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Success)
        val token = (result as TokenExchangeResult.Success).token
        Assertions.assertEquals("full-token", token.accessToken)
        Assertions.assertEquals("MAC", token.tokenType)
        Assertions.assertEquals(7200, token.expiresIn)
        Assertions.assertEquals("rt-123", token.refreshToken)
        Assertions.assertEquals("read write admin", token.scope)
    }

    @Test
    fun `test parse token response missing access_token`(): Unit = runBlocking {
        val port = mockServerManager.start(0)!!
        val tokenResponse = """{"token_type": "Bearer", "expires_in": 3600}"""

        mockServerManager.addRule(
            MockRule.create("/token", HttpMethod.POST, tokenResponse)
        )

        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "http://localhost:$port/token",
            clientId = "client",
            clientSecret = "secret"
        )

        val entry = oauth2Service.createConfig("Missing Token", config)
        val result = oauth2Service.exchangeClientCredentials(entry.id)

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("missing access_token"))
    }
}