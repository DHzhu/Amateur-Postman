package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.google.gson.JsonParser
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Result of a token exchange operation.
 */
sealed class TokenExchangeResult {
    data class Success(val token: OAuth2Token) : TokenExchangeResult()
    data class Error(val message: String, val statusCode: Int? = null) : TokenExchangeResult()
}

/**
 * Service for managing OAuth 2.0 configurations and tokens with persistent storage.
 *
 * Provides:
 * - CRUD operations for OAuth2 configurations
 * - Token lifecycle management (storage, refresh, expiration)
 * - Per-request and per-collection auth configuration
 */
@Service(Service.Level.PROJECT)
@State(name = "OAuth2Service", storages = [Storage("amateur-postman-oauth2.xml")])
class OAuth2Service(private val project: Project) : PersistentStateComponent<OAuth2State> {

    private val logger = thisLogger()
    private var state = OAuth2State()
    private val listeners = mutableListOf<OAuth2ConfigChangeListener>()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override fun getState(): OAuth2State = state

    override fun loadState(state: OAuth2State) {
        this.state = state
        logger.info("Loaded ${state.configs.size} OAuth2 configurations")
    }

    // ========== Configuration CRUD Operations ==========

    /**
     * Creates a new OAuth2 configuration.
     */
    fun createConfig(name: String, config: OAuth2Config): OAuth2ConfigEntry {
        val id = java.util.UUID.randomUUID().toString()
        val entry = OAuth2ConfigEntry(id = id, name = name, config = config)
        state = state.copy(configs = state.configs + entry)
        logger.info("Created OAuth2 config: $name ($id)")
        notifyListeners()
        return entry
    }

    /**
     * Gets an OAuth2 configuration by ID.
     */
    fun getConfig(id: String): OAuth2ConfigEntry? = state.configs.find { it.id == id }

    /**
     * Gets all OAuth2 configurations.
     */
    fun getAllConfigs(): List<OAuth2ConfigEntry> = state.configs

    /**
     * Updates an existing OAuth2 configuration.
     */
    fun updateConfig(id: String, config: OAuth2Config): Boolean {
        val index = state.configs.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updatedList = state.configs.toMutableList()
            updatedList[index] = updatedList[index].copy(config = config)
            state = state.copy(configs = updatedList)
            logger.info("Updated OAuth2 config: $id")
            notifyListeners()
            return true
        }
        return false
    }

    /**
     * Renames an OAuth2 configuration.
     */
    fun renameConfig(id: String, newName: String): Boolean {
        val index = state.configs.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updatedList = state.configs.toMutableList()
            updatedList[index] = updatedList[index].copy(name = newName)
            state = state.copy(configs = updatedList)
            logger.info("Renamed OAuth2 config to: $newName")
            notifyListeners()
            return true
        }
        return false
    }

    /**
     * Deletes an OAuth2 configuration.
     */
    fun deleteConfig(id: String): Boolean {
        val initialSize = state.configs.size
        state = state.copy(configs = state.configs.filter { it.id != id })
        if (state.configs.size < initialSize) {
            state = state.copy(requestAuthMappings = state.requestAuthMappings.filter { it.configId != id })
            logger.info("Deleted OAuth2 config: $id")
            notifyListeners()
            return true
        }
        return false
    }

    // ========== Token Management ==========

    /**
     * Stores a token for a configuration.
     */
    fun setToken(configId: String, token: OAuth2Token): Boolean {
        val index = state.configs.indexOfFirst { it.id == configId }
        if (index >= 0) {
            val updatedList = state.configs.toMutableList()
            updatedList[index] = updatedList[index].copy(
                config = updatedList[index].config.copy(accessToken = token)
            )
            state = state.copy(configs = updatedList)
            logger.info("Stored token for config: $configId")
            notifyListeners()
            return true
        }
        return false
    }

    /**
     * Gets the token for a configuration.
     */
    fun getToken(configId: String): OAuth2Token? = getConfig(configId)?.config?.accessToken

    /**
     * Clears the token for a configuration.
     */
    fun clearToken(configId: String): Boolean {
        val index = state.configs.indexOfFirst { it.id == configId }
        if (index >= 0) {
            val currentEntry = state.configs[index]
            if (currentEntry.config.accessToken != null) {
                val updatedList = state.configs.toMutableList()
                updatedList[index] = currentEntry.copy(
                    config = currentEntry.config.copy(accessToken = null)
                )
                state = state.copy(configs = updatedList)
                logger.info("Cleared token for config: $configId")
                notifyListeners()
            }
            return true
        }
        return false
    }

    /**
     * Checks if a configuration has a valid (non-expired) token.
     */
    fun hasValidToken(configId: String): Boolean {
        val token = getToken(configId) ?: return false
        return !token.isExpired()
    }

    // ========== Token Exchange Operations ==========

    /**
     * Exchanges credentials for a new token using Client Credentials flow.
     *
     * @param configId The configuration ID
     * @return TokenExchangeResult containing the token or error
     */
    suspend fun exchangeClientCredentials(configId: String): TokenExchangeResult = withContext(Dispatchers.IO) {
        val entry = getConfig(configId)
        if (entry == null) {
            return@withContext TokenExchangeResult.Error("Configuration not found: $configId")
        }

        val config = entry.config
        if (config.grantType != OAuth2GrantType.CLIENT_CREDENTIALS) {
            return@withContext TokenExchangeResult.Error("Invalid grant type: expected CLIENT_CREDENTIALS")
        }

        if (config.clientSecret.isNullOrBlank()) {
            return@withContext TokenExchangeResult.Error("Client secret is required for Client Credentials flow")
        }

        try {
            logger.info("Exchanging client credentials at ${config.tokenUrl}")

            val formBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .apply {
                    config.scope?.let { add("scope", it) }
                }
                .build()

            val request = Request.Builder()
                .url(config.tokenUrl)
                .header("Authorization", buildBasicAuth(config.clientId, config.clientSecret!!))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    logger.warn("Token exchange failed: ${response.code} - $responseBody")
                    return@withContext TokenExchangeResult.Error(
                        "Token exchange failed: ${response.code} ${response.message}",
                        response.code
                    )
                }

                parseTokenResponse(responseBody)
            }
        } catch (e: Exception) {
            logger.error("Token exchange error", e)
            TokenExchangeResult.Error("Token exchange failed: ${e.message}")
        }
    }

    /**
     * Exchanges credentials for a new token using Password flow.
     *
     * @param configId The configuration ID
     * @return TokenExchangeResult containing the token or error
     */
    suspend fun exchangePassword(configId: String): TokenExchangeResult = withContext(Dispatchers.IO) {
        val entry = getConfig(configId)
        if (entry == null) {
            return@withContext TokenExchangeResult.Error("Configuration not found: $configId")
        }

        val config = entry.config
        if (config.grantType != OAuth2GrantType.PASSWORD) {
            return@withContext TokenExchangeResult.Error("Invalid grant type: expected PASSWORD")
        }

        if (config.username.isNullOrBlank() || config.password.isNullOrBlank()) {
            return@withContext TokenExchangeResult.Error("Username and password are required for Password flow")
        }

        try {
            logger.info("Exchanging password credentials at ${config.tokenUrl}")

            val formBodyBuilder = FormBody.Builder()
                .add("grant_type", "password")
                .add("username", config.username)
                .add("password", config.password)

            config.scope?.let { formBodyBuilder.add("scope", it) }

            val requestBuilder = Request.Builder()
                .url(config.tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBodyBuilder.build())

            // Add Basic Auth if client credentials are provided
            if (!config.clientId.isNullOrBlank() && !config.clientSecret.isNullOrBlank()) {
                requestBuilder.header("Authorization", buildBasicAuth(config.clientId, config.clientSecret))
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    logger.warn("Password token exchange failed: ${response.code} - $responseBody")
                    return@withContext TokenExchangeResult.Error(
                        "Token exchange failed: ${response.code} ${response.message}",
                        response.code
                    )
                }

                parseTokenResponse(responseBody)
            }
        } catch (e: Exception) {
            logger.error("Password token exchange error", e)
            TokenExchangeResult.Error("Token exchange failed: ${e.message}")
        }
    }

    /**
     * Refreshes an expired token using the refresh token.
     *
     * @param configId The configuration ID
     * @return TokenExchangeResult containing the new token or error
     */
    suspend fun refreshToken(configId: String): TokenExchangeResult = withContext(Dispatchers.IO) {
        val entry = getConfig(configId)
        if (entry == null) {
            return@withContext TokenExchangeResult.Error("Configuration not found: $configId")
        }

        val currentToken = entry.config.accessToken
        if (currentToken?.refreshToken.isNullOrBlank()) {
            return@withContext TokenExchangeResult.Error("No refresh token available")
        }

        val config = entry.config

        try {
            logger.info("Refreshing token at ${config.tokenUrl}")

            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", currentToken!!.refreshToken!!)
                .build()

            val requestBuilder = Request.Builder()
                .url(config.tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)

            // Add Basic Auth if client credentials are provided
            if (!config.clientId.isNullOrBlank() && !config.clientSecret.isNullOrBlank()) {
                requestBuilder.header("Authorization", buildBasicAuth(config.clientId, config.clientSecret))
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    logger.warn("Token refresh failed: ${response.code} - $responseBody")
                    return@withContext TokenExchangeResult.Error(
                        "Token refresh failed: ${response.code} ${response.message}",
                        response.code
                    )
                }

                parseTokenResponse(responseBody)
            }
        } catch (e: Exception) {
            logger.error("Token refresh error", e)
            TokenExchangeResult.Error("Token refresh failed: ${e.message}")
        }
    }

    /**
     * Fetches a new token and stores it.
     * Automatically selects the appropriate flow based on configuration.
     */
    suspend fun fetchAndStoreToken(configId: String): TokenExchangeResult {
        val entry = getConfig(configId) ?: return TokenExchangeResult.Error("Configuration not found")

        val result = when (entry.config.grantType) {
            OAuth2GrantType.CLIENT_CREDENTIALS -> exchangeClientCredentials(configId)
            OAuth2GrantType.PASSWORD -> exchangePassword(configId)
            else -> TokenExchangeResult.Error("Grant type ${entry.config.grantType} requires interactive authorization")
        }

        if (result is TokenExchangeResult.Success) {
            setToken(configId, result.token)
        }

        return result
    }

    // ========== Helper Methods ==========

    private fun buildBasicAuth(clientId: String, clientSecret: String): String {
        val credentials = "$clientId:$clientSecret"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encoded"
    }

    private fun parseTokenResponse(responseBody: String): TokenExchangeResult {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject

            val accessToken = json.get("access_token")?.asString
            if (accessToken.isNullOrBlank()) {
                return TokenExchangeResult.Error("Invalid token response: missing access_token")
            }

            val tokenType = json.get("token_type")?.asString ?: "Bearer"
            val expiresIn = json.get("expires_in")?.asLong
            val refreshToken = json.get("refresh_token")?.asString
            val scope = json.get("scope")?.asString

            val token = OAuth2Token(
                accessToken = accessToken,
                tokenType = tokenType,
                expiresIn = expiresIn,
                refreshToken = refreshToken,
                scope = scope
            )

            logger.info("Successfully parsed token response")
            TokenExchangeResult.Success(token)
        } catch (e: Exception) {
            logger.error("Failed to parse token response", e)
            TokenExchangeResult.Error("Failed to parse token response: ${e.message}")
        }
    }

    // ========== Request-Config Mapping ==========

    /**
     * Associates an OAuth2 configuration with a request.
     */
    fun setRequestAuthConfig(requestId: String, configId: String?) {
        val existingIndex = state.requestAuthMappings.indexOfFirst { it.requestId == requestId }

        if (configId == null) {
            if (existingIndex >= 0) {
                state = state.copy(
                    requestAuthMappings = state.requestAuthMappings.filter { it.requestId != requestId }
                )
                logger.debug("Removed auth mapping for request: $requestId")
            }
        } else {
            val mapping = RequestAuthMapping(requestId, configId)
            val updatedMappings = if (existingIndex >= 0) {
                state.requestAuthMappings.toMutableList().apply { set(existingIndex, mapping) }
            } else {
                state.requestAuthMappings + mapping
            }
            state = state.copy(requestAuthMappings = updatedMappings)
            logger.debug("Set auth config $configId for request: $requestId")
        }
        notifyListeners()
    }

    /**
     * Gets the OAuth2 configuration for a request.
     */
    fun getRequestAuthConfig(requestId: String): OAuth2ConfigEntry? {
        val mapping = state.requestAuthMappings.find { it.requestId == requestId } ?: return null
        return getConfig(mapping.configId)
    }

    /**
     * Gets the OAuth2Auth for a request, including the current token.
     */
    fun getRequestOAuth2Auth(requestId: String): OAuth2Auth? {
        val entry = getRequestAuthConfig(requestId) ?: return null
        val token = entry.config.accessToken ?: return null
        return OAuth2Auth(entry.config, token)
    }

    // ========== Collection-Level Auth ==========

    /**
     * Associates an OAuth2 configuration with a collection.
     */
    fun setCollectionAuthConfig(collectionId: String, configId: String?) {
        val existingIndex = state.collectionAuthMappings.indexOfFirst { it.collectionId == collectionId }

        if (configId == null) {
            if (existingIndex >= 0) {
                state = state.copy(
                    collectionAuthMappings = state.collectionAuthMappings.filter { it.collectionId != collectionId }
                )
                logger.debug("Removed auth mapping for collection: $collectionId")
            }
        } else {
            val mapping = CollectionAuthMapping(collectionId, configId)
            val updatedMappings = if (existingIndex >= 0) {
                state.collectionAuthMappings.toMutableList().apply { set(existingIndex, mapping) }
            } else {
                state.collectionAuthMappings + mapping
            }
            state = state.copy(collectionAuthMappings = updatedMappings)
            logger.debug("Set auth config $configId for collection: $collectionId")
        }
        notifyListeners()
    }

    /**
     * Gets the OAuth2 configuration for a collection.
     */
    fun getCollectionAuthConfig(collectionId: String): OAuth2ConfigEntry? {
        val mapping = state.collectionAuthMappings.find { it.collectionId == collectionId } ?: return null
        return getConfig(mapping.configId)
    }

    // ========== Change Listeners ==========

    fun addChangeListener(listener: OAuth2ConfigChangeListener) = listeners.add(listener)

    fun removeChangeListener(listener: OAuth2ConfigChangeListener) = listeners.remove(listener)

    private fun notifyListeners() = listeners.forEach { it.onOAuth2ConfigChanged() }
}

// ========== State and Data Classes ==========

data class OAuth2State(
    val version: Int = 1,
    val configs: List<OAuth2ConfigEntry> = emptyList(),
    val requestAuthMappings: List<RequestAuthMapping> = emptyList(),
    val collectionAuthMappings: List<CollectionAuthMapping> = emptyList()
)

data class OAuth2ConfigEntry(
    val id: String,
    val name: String,
    val config: OAuth2Config
)

data class RequestAuthMapping(
    val requestId: String,
    val configId: String
)

data class CollectionAuthMapping(
    val collectionId: String,
    val configId: String
)

interface OAuth2ConfigChangeListener {
    fun onOAuth2ConfigChanged()
}