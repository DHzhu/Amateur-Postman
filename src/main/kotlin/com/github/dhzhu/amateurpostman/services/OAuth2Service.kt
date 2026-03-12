package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

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