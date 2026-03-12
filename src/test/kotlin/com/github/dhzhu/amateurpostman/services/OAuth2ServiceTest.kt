package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * Tests for OAuth2Service persistence and token management.
 */
class OAuth2ServiceTest {

    private lateinit var service: OAuth2Service

    @BeforeEach
    fun setUp() {
        service = OAuth2Service(mock<Project>())
    }

    // ========== Configuration CRUD Tests ==========

    @Test
    fun testCreateConfig() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client-id",
            clientSecret = "test-secret"
        )

        val entry = service.createConfig("Test Config", config)

        assertNotNull(entry.id)
        assertEquals("Test Config", entry.name)
        assertEquals(OAuth2GrantType.CLIENT_CREDENTIALS, entry.config.grantType)
        assertEquals("https://auth.example.com/token", entry.config.tokenUrl)
    }

    @Test
    fun testGetConfig() {
        val config = createTestConfig()
        val created = service.createConfig("My Config", config)

        val retrieved = service.getConfig(created.id)

        assertNotNull(retrieved)
        assertEquals(created.id, retrieved!!.id)
        assertEquals("My Config", retrieved.name)
    }

    @Test
    fun testGetConfigNotFound() {
        val retrieved = service.getConfig("non-existent-id")
        assertNull(retrieved)
    }

    @Test
    fun testGetAllConfigs() {
        service.createConfig("Config 1", createTestConfig())
        service.createConfig("Config 2", createTestConfig())

        val allConfigs = service.getAllConfigs()

        assertEquals(2, allConfigs.size)
    }

    @Test
    fun testUpdateConfig() {
        val created = service.createConfig("Original", createTestConfig())

        val updatedConfig = createTestConfig().copy(clientId = "updated-client-id")
        val result = service.updateConfig(created.id, updatedConfig)

        assertTrue(result)
        val retrieved = service.getConfig(created.id)
        assertEquals("updated-client-id", retrieved!!.config.clientId)
    }

    @Test
    fun testUpdateConfigNotFound() {
        val result = service.updateConfig("non-existent", createTestConfig())
        assertFalse(result)
    }

    @Test
    fun testRenameConfig() {
        val created = service.createConfig("Old Name", createTestConfig())

        val result = service.renameConfig(created.id, "New Name")

        assertTrue(result)
        val retrieved = service.getConfig(created.id)
        assertEquals("New Name", retrieved!!.name)
    }

    @Test
    fun testDeleteConfig() {
        val created = service.createConfig("To Delete", createTestConfig())

        val result = service.deleteConfig(created.id)

        assertTrue(result)
        assertNull(service.getConfig(created.id))
        assertEquals(0, service.getAllConfigs().size)
    }

    @Test
    fun testDeleteConfigNotFound() {
        val result = service.deleteConfig("non-existent")
        assertFalse(result)
    }

    // ========== Token Management Tests ==========

    @Test
    fun testSetToken() {
        val created = service.createConfig("Test", createTestConfig())
        val token = OAuth2Token(
            accessToken = "access-token-123",
            tokenType = "Bearer",
            expiresIn = 3600
        )

        val result = service.setToken(created.id, token)

        assertTrue(result)
        val retrievedToken = service.getToken(created.id)
        assertNotNull(retrievedToken)
        assertEquals("access-token-123", retrievedToken!!.accessToken)
        assertEquals("Bearer", retrievedToken.tokenType)
    }

    @Test
    fun testGetTokenNoConfig() {
        val token = service.getToken("non-existent")
        assertNull(token)
    }

    @Test
    fun testClearToken() {
        val created = service.createConfig("Test", createTestConfig())
        val token = OAuth2Token(accessToken = "test-token", expiresIn = 3600)
        service.setToken(created.id, token)

        val result = service.clearToken(created.id)

        assertTrue(result)
        assertNull(service.getToken(created.id))
    }

    @Test
    fun testHasValidToken() {
        val created = service.createConfig("Test", createTestConfig())
        val validToken = OAuth2Token(
            accessToken = "valid-token",
            expiresIn = 3600 // 1 hour
        )

        service.setToken(created.id, validToken)
        assertTrue(service.hasValidToken(created.id))
    }

    @Test
    fun testHasValidTokenExpired() {
        val created = service.createConfig("Test", createTestConfig())
        val expiredToken = OAuth2Token(
            accessToken = "expired-token",
            expiresIn = 0,
            createdAt = 0 // Very old
        )

        service.setToken(created.id, expiredToken)
        assertFalse(service.hasValidToken(created.id))
    }

    @Test
    fun testHasValidTokenNoToken() {
        val created = service.createConfig("Test", createTestConfig())
        assertFalse(service.hasValidToken(created.id))
    }

    // ========== Request-Config Mapping Tests ==========

    @Test
    fun testSetRequestAuthConfig() {
        val config = service.createConfig("Test", createTestConfig())

        service.setRequestAuthConfig("request-123", config.id)

        val retrieved = service.getRequestAuthConfig("request-123")
        assertNotNull(retrieved)
        assertEquals(config.id, retrieved!!.id)
    }

    @Test
    fun testSetRequestAuthConfigNull() {
        val config = service.createConfig("Test", createTestConfig())
        service.setRequestAuthConfig("request-123", config.id)

        service.setRequestAuthConfig("request-123", null)

        assertNull(service.getRequestAuthConfig("request-123"))
    }

    @Test
    fun testGetRequestOAuth2Auth() {
        val config = service.createConfig("Test", createTestConfig())
        val token = OAuth2Token(accessToken = "test-access-token", expiresIn = 3600)
        service.setToken(config.id, token)
        service.setRequestAuthConfig("request-123", config.id)

        val auth = service.getRequestOAuth2Auth("request-123")

        assertNotNull(auth)
        val headers = auth!!.toHeaders()
        assertEquals("Bearer test-access-token", headers["Authorization"])
    }

    @Test
    fun testGetRequestOAuth2AuthNoToken() {
        val config = service.createConfig("Test", createTestConfig())
        service.setRequestAuthConfig("request-123", config.id)

        val auth = service.getRequestOAuth2Auth("request-123")

        assertNull(auth) // No token stored
    }

    @Test
    fun testGetRequestOAuth2AuthNoMapping() {
        val auth = service.getRequestOAuth2Auth("unknown-request")
        assertNull(auth)
    }

    // ========== Collection-Level Auth Tests ==========

    @Test
    fun testSetCollectionAuthConfig() {
        val config = service.createConfig("Test", createTestConfig())

        service.setCollectionAuthConfig("collection-123", config.id)

        val retrieved = service.getCollectionAuthConfig("collection-123")
        assertNotNull(retrieved)
        assertEquals(config.id, retrieved!!.id)
    }

    @Test
    fun testSetCollectionAuthConfigNull() {
        val config = service.createConfig("Test", createTestConfig())
        service.setCollectionAuthConfig("collection-123", config.id)

        service.setCollectionAuthConfig("collection-123", null)

        assertNull(service.getCollectionAuthConfig("collection-123"))
    }

    // ========== Change Listener Tests ==========

    @Test
    fun testChangeListenerOnCreate() {
        var changeCount = 0
        service.addChangeListener(object : OAuth2ConfigChangeListener {
            override fun onOAuth2ConfigChanged() { changeCount++ }
        })

        service.createConfig("Test", createTestConfig())

        assertEquals(1, changeCount)
    }

    @Test
    fun testChangeListenerOnUpdate() {
        val config = service.createConfig("Test", createTestConfig())
        var changeCount = 0
        service.addChangeListener(object : OAuth2ConfigChangeListener {
            override fun onOAuth2ConfigChanged() { changeCount++ }
        })

        service.updateConfig(config.id, createTestConfig())

        assertEquals(1, changeCount)
    }

    @Test
    fun testChangeListenerOnDelete() {
        val config = service.createConfig("Test", createTestConfig())
        var changeCount = 0
        service.addChangeListener(object : OAuth2ConfigChangeListener {
            override fun onOAuth2ConfigChanged() { changeCount++ }
        })

        service.deleteConfig(config.id)

        assertEquals(1, changeCount)
    }

    @Test
    fun testRemoveChangeListener() {
        var changeCount = 0
        val listener = object : OAuth2ConfigChangeListener {
            override fun onOAuth2ConfigChanged() { changeCount++ }
        }
        service.addChangeListener(listener)
        service.removeChangeListener(listener)

        service.createConfig("Test", createTestConfig())

        assertEquals(0, changeCount)
    }

    // ========== OAuth2GrantType Tests ==========

    @Test
    fun testAuthorizationCodeConfig() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.AUTHORIZATION_CODE,
            authUrl = "https://auth.example.com/authorize",
            tokenUrl = "https://auth.example.com/token",
            clientId = "auth-code-client",
            clientSecret = "secret",
            redirectUri = "http://localhost:8080/callback",
            scope = "read write"
        )

        val entry = service.createConfig("Auth Code", config)

        assertEquals(OAuth2GrantType.AUTHORIZATION_CODE, entry.config.grantType)
        assertEquals("https://auth.example.com/authorize", entry.config.authUrl)
        assertEquals("http://localhost:8080/callback", entry.config.redirectUri)
    }

    @Test
    fun testClientCredentialsConfig() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "cc-client",
            clientSecret = "secret",
            scope = "api"
        )

        val entry = service.createConfig("Client Credentials", config)

        assertEquals(OAuth2GrantType.CLIENT_CREDENTIALS, entry.config.grantType)
        assertNull(entry.config.authUrl) // Not needed for client credentials
    }

    @Test
    fun testPasswordConfig() {
        val config = OAuth2Config(
            grantType = OAuth2GrantType.PASSWORD,
            tokenUrl = "https://auth.example.com/token",
            clientId = "password-client",
            username = "testuser",
            password = "testpass"
        )

        val entry = service.createConfig("Password", config)

        assertEquals(OAuth2GrantType.PASSWORD, entry.config.grantType)
        assertEquals("testuser", entry.config.username)
        assertEquals("testpass", entry.config.password)
    }

    // ========== Helper Methods ==========

    private fun createTestConfig() = OAuth2Config(
        grantType = OAuth2GrantType.CLIENT_CREDENTIALS,
        tokenUrl = "https://auth.example.com/token",
        clientId = "test-client",
        clientSecret = "test-secret"
    )
}