package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.mockito.kotlin.mock
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Tests for OAuth 2.0 browser-based authorization flows.
 */
class OAuth2AuthorizationFlowTest {

    private lateinit var oauth2Service: OAuth2Service

    @BeforeEach
    fun setUp() {
        oauth2Service = OAuth2Service(mock<Project>())
    }

    // ========== Authorization URL Generation Tests ==========

    @Test
    fun testGenerateAuthCodeUrl() {
        val url = OAuth2AuthorizationUrl.generateAuthCodeUrl(
            authUrl = "https://auth.example.com/authorize",
            clientId = "test-client",
            redirectUri = "http://localhost:8080/callback",
            scope = "read write",
            state = "test-state-123"
        )

        Assertions.assertTrue(url.contains("https://auth.example.com/authorize"))
        Assertions.assertTrue(url.contains("response_type=code"))
        Assertions.assertTrue(url.contains("client_id=test-client"))
        Assertions.assertTrue(url.contains("redirect_uri="))
        Assertions.assertTrue(url.contains("scope="))
        Assertions.assertTrue(url.contains("state=test-state-123"))
    }

    @Test
    fun testGenerateAuthCodeUrlWithAdditionalParams() {
        val url = OAuth2AuthorizationUrl.generateAuthCodeUrl(
            authUrl = "https://auth.example.com/authorize",
            clientId = "client",
            redirectUri = "http://localhost:8080/callback",
            additionalParams = mapOf("prompt" to "consent", "audience" to "https://api.example.com")
        )

        Assertions.assertTrue(url.contains("prompt=consent"))
        Assertions.assertTrue(url.contains("audience="))
    }

    @Test
    fun testGenerateImplicitUrl() {
        val url = OAuth2AuthorizationUrl.generateImplicitUrl(
            authUrl = "https://auth.example.com/authorize",
            clientId = "test-client",
            redirectUri = "http://localhost:8080/callback",
            scope = "profile"
        )

        Assertions.assertTrue(url.contains("response_type=token"))
        Assertions.assertTrue(url.contains("client_id=test-client"))
    }

    @Test
    fun testGenerateAuthCodeUrlWithoutScope() {
        val url = OAuth2AuthorizationUrl.generateAuthCodeUrl(
            authUrl = "https://auth.example.com/authorize",
            clientId = "client",
            redirectUri = "http://localhost:8080/callback"
        )

        Assertions.assertFalse(url.contains("scope="))
    }

    // ========== Token Parser Tests ==========

    @Test
    fun testParseTokenFromFragment() {
        val fragment = "access_token=test-token-123&token_type=Bearer&expires_in=3600&scope=read"

        val params = OAuth2TokenParser.parseFromFragment(fragment)

        Assertions.assertEquals("test-token-123", params["access_token"])
        Assertions.assertEquals("Bearer", params["token_type"])
        Assertions.assertEquals("3600", params["expires_in"])
        Assertions.assertEquals("read", params["scope"])
    }

    @Test
    fun testParseTokenFromFragmentWithLeadingHash() {
        val fragment = "#access_token=my-token&token_type=Bearer"

        val params = OAuth2TokenParser.parseFromFragment(fragment)

        Assertions.assertEquals("my-token", params["access_token"])
    }

    @Test
    fun testExtractAccessToken() {
        val fragment = "access_token=extracted-token&token_type=Bearer"

        val token = OAuth2TokenParser.extractAccessToken(fragment)

        Assertions.assertEquals("extracted-token", token)
    }

    @Test
    fun testExtractAccessTokenEmptyFragment() {
        val token = OAuth2TokenParser.extractAccessToken("")
        Assertions.assertNull(token)
    }

    @Test
    fun testExtractError() {
        val fragment = "error=access_denied&error_description=User+denied+access"

        val error = OAuth2TokenParser.extractError(fragment)

        Assertions.assertNotNull(error)
        Assertions.assertEquals("access_denied", error!!.first)
        Assertions.assertEquals("User denied access", error.second)
    }

    @Test
    fun testExtractErrorNoError() {
        val fragment = "access_token=token"
        val error = OAuth2TokenParser.extractError(fragment)
        Assertions.assertNull(error)
    }

    // ========== Callback Server Tests ==========

    @Test
    fun testCallbackServerStart() {
        val server = OAuth2CallbackServer(port = 0)
        val started = server.start()

        Assertions.assertTrue(started)
        Assertions.assertTrue(server.actualPort > 0)
        Assertions.assertEquals("http://localhost:${server.actualPort}/callback", server.redirectUri)

        server.stop()
    }

    @Test
    fun testCallbackServerReceivesCallback() {
        val server = OAuth2CallbackServer(port = 0, timeoutSeconds = 5)
        Assertions.assertTrue(server.start())

        val port = server.actualPort
        val latch = CountDownLatch(1)

        // Make HTTP request to callback endpoint in a separate thread
        Thread {
            try {
                TimeUnit.MILLISECONDS.sleep(100) // Wait for server to be ready
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("http://localhost:$port/callback?code=test-auth-code&state=test-state")
                    .get()
                    .build()
                client.newCall(request).execute().use { latch.countDown() }
            } catch (e: Exception) {
                latch.countDown()
            }
        }.start()

        latch.await(2, TimeUnit.SECONDS)
        server.stop()
    }

    // ========== Authorization Code Flow Tests ==========

    @Test
    fun testStartAuthorizationCodeFlowInvalidGrantType() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client"
        )

        val entry = oauth2Service.createConfig("Test", config)
        val result = oauth2Service.startAuthorizationCodeFlow(entry.id)

        Assertions.assertNull(result)
    }

    @Test
    fun testStartAuthorizationCodeFlowMissingAuthUrl() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            authUrl = null,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            redirectUri = "http://localhost:8080/callback"
        )

        val entry = oauth2Service.createConfig("Missing Auth URL", config)
        val result = oauth2Service.startAuthorizationCodeFlow(entry.id)

        Assertions.assertNull(result)
    }

    @Test
    fun testStartAuthorizationCodeFlowSuccess() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            authUrl = "https://auth.example.com/authorize",
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client",
            redirectUri = "http://localhost:0/callback",
            scope = "read write"
        )

        val entry = oauth2Service.createConfig("Auth Code Config", config)
        val result = oauth2Service.startAuthorizationCodeFlow(entry.id)

        Assertions.assertNotNull(result)
        val (authUrl, server) = result!!

        Assertions.assertTrue(authUrl.contains("response_type=code"))
        Assertions.assertTrue(authUrl.contains("client_id=test-client"))
        Assertions.assertTrue(authUrl.contains("scope=read+write"))

        server.stop()
    }

    @Test
    fun testStartAuthorizationCodeFlowConfigNotFound() {
        val result = oauth2Service.startAuthorizationCodeFlow("non-existent")
        Assertions.assertNull(result)
    }

    // ========== Implicit Flow Tests ==========

    @Test
    fun testStartImplicitFlowSuccess() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.IMPLICIT,
            authUrl = "https://auth.example.com/authorize",
            tokenUrl = "https://auth.example.com/token",
            clientId = "implicit-client",
            redirectUri = "http://localhost:8080/callback",
            scope = "profile email"
        )

        val entry = oauth2Service.createConfig("Implicit Config", config)
        val authUrl = oauth2Service.startImplicitFlow(entry.id)

        Assertions.assertNotNull(authUrl)
        Assertions.assertTrue(authUrl!!.contains("response_type=token"))
        Assertions.assertTrue(authUrl.contains("client_id=implicit-client"))
        Assertions.assertTrue(authUrl.contains("scope=profile+email"))
    }

    @Test
    fun testStartImplicitFlowInvalidGrantType() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "client"
        )

        val entry = oauth2Service.createConfig("Wrong Grant", config)
        val result = oauth2Service.startImplicitFlow(entry.id)

        Assertions.assertNull(result)
    }

    @Test
    fun testParseAndStoreImplicitTokenSuccess() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.IMPLICIT,
            authUrl = "https://auth.example.com/authorize",
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            redirectUri = "http://localhost:8080/callback"
        )

        val entry = oauth2Service.createConfig("Implicit", config)

        val callbackUrl = "http://localhost:8080/callback#access_token=implicit-token-456&token_type=Bearer&expires_in=7200"

        val result = oauth2Service.parseAndStoreImplicitToken(entry.id, callbackUrl)

        Assertions.assertTrue(result is TokenExchangeResult.Success)
        val token = (result as TokenExchangeResult.Success).token
        Assertions.assertEquals("implicit-token-456", token.accessToken)
        Assertions.assertEquals("Bearer", token.tokenType)
        Assertions.assertEquals(7200, token.expiresIn)

        // Verify token was stored
        val storedToken = oauth2Service.getToken(entry.id)
        Assertions.assertNotNull(storedToken)
        Assertions.assertEquals("implicit-token-456", storedToken?.accessToken)
    }

    @Test
    fun testParseAndStoreImplicitTokenWithError() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.IMPLICIT,
            authUrl = "https://auth.example.com/authorize",
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            redirectUri = "http://localhost:8080/callback"
        )

        val entry = oauth2Service.createConfig("Error Implicit", config)

        val callbackUrl = "http://localhost:8080/callback#error=access_denied&error_description=User+cancelled"

        val result = oauth2Service.parseAndStoreImplicitToken(entry.id, callbackUrl)

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("access_denied"))
    }

    @Test
    fun testParseAndStoreImplicitTokenNoFragment() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.IMPLICIT,
            authUrl = "https://auth.example.com/authorize",
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            redirectUri = "http://localhost:8080/callback"
        )

        val entry = oauth2Service.createConfig("No Fragment", config)

        val callbackUrl = "http://localhost:8080/callback"

        val result = oauth2Service.parseAndStoreImplicitToken(entry.id, callbackUrl)

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("No token"))
    }

    @Test
    fun testParseAndStoreImplicitTokenConfigNotFound() {
        val result = oauth2Service.parseAndStoreImplicitToken("non-existent", "http://localhost:8080/callback#access_token=token")
        Assertions.assertTrue(result is TokenExchangeResult.Error)
    }

    // ========== Exchange Authorization Code Tests ==========

    @Test
    fun testExchangeAuthorizationCodeConfigNotFound(): Unit = runBlocking {
        val result = oauth2Service.exchangeAuthorizationCode("non-existent", "code-123")
        Assertions.assertTrue(result is TokenExchangeResult.Error)
    }

    @Test
    fun testExchangeAuthorizationCodeMissingRedirectUri(): Unit = runBlocking {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            authUrl = "https://auth.example.com/authorize",
            tokenUrl = "https://auth.example.com/token",
            clientId = "client",
            redirectUri = null
        )

        val entry = oauth2Service.createConfig("No Redirect", config)
        val result = oauth2Service.exchangeAuthorizationCode(entry.id, "code-123")

        Assertions.assertTrue(result is TokenExchangeResult.Error)
        val error = result as TokenExchangeResult.Error
        Assertions.assertTrue(error.message.contains("Redirect URI is required"))
    }
}