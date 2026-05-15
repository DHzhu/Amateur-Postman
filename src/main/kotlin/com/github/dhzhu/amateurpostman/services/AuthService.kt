package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking

/**
 * Service for resolving authentication with inheritance support.
 *
 * Authentication is resolved in the following order (highest priority first):
 * 1. Request-level authentication
 * 2. Parent folder authentication (immediate parent first)
 * 3. Collection-level authentication
 * 4. No authentication
 *
 * OAuth2 configurations are resolved via OAuth2Service with auto-refresh support.
 */
@Service(Service.Level.PROJECT)
class AuthService(private val project: Project) {

    private val logger = thisLogger()

    /**
     * Resolves the effective authentication for a request item.
     *
     * @param collectionId The collection ID
     * @param itemId The request item ID
     * @return The resolved Authentication, or null if no auth is configured
     */
    suspend fun resolveAuth(collectionId: String, itemId: String): Authentication? {
        val collectionService = project.service<CollectionService>()
        val collection = collectionService.getCollection(collectionId) ?: return null

        // Find the request item
        val requestItem = collection.findItemById(itemId) as? CollectionItem.Request ?: return null

        // 1. Check request-level auth first
        val requestAuth = requestItem.request.authentication
        if (requestAuth != null && requestAuth !is OAuth2ConfigRef) {
            logger.debug("Using request-level auth for item: $itemId")
            return requestAuth
        }

        // 2. Check folder inheritance (walk up the hierarchy)
        val inheritedAuth = resolveInheritedAuth(collection, requestItem.parentId)
        if (inheritedAuth != null) {
            return inheritedAuth
        }

        // 3. Check collection-level auth
        val collectionAuth = collection.auth?.toAuthentication()
        if (collectionAuth != null && collectionAuth !is OAuth2ConfigRef) {
            logger.debug("Using collection-level auth for item: $itemId")
            return resolveOAuth2Ref(collectionAuth)
        }

        // 4. No auth
        logger.debug("No auth configured for item: $itemId")
        return null
    }

    /**
     * Resolves inherited authentication by walking up the folder hierarchy.
     *
     * @param collection The collection
     * @param folderId The starting folder ID (immediate parent of request)
     * @return The resolved Authentication, or null if no inherited auth found
     */
    private suspend fun resolveInheritedAuth(
        collection: RequestCollection,
        folderId: String?
    ): Authentication? {
        if (folderId == null) return null

        // Build folder chain from immediate parent up to collection root
        val folderChain = buildFolderChain(collection.items, folderId)

        // Walk from immediate parent to root, first auth found wins
        for (folder in folderChain) {
            val folderAuth = folder.auth?.toAuthentication()
            if (folderAuth != null && folderAuth !is OAuth2ConfigRef) {
                logger.debug("Found auth in folder: ${folder.name}")
                return resolveOAuth2Ref(folderAuth)
            }
            // Check for OAuth2ConfigRef
            if (folderAuth is OAuth2ConfigRef) {
                val resolved = resolveOAuth2ConfigRef(folderAuth)
                if (resolved != null) {
                    logger.debug("Resolved OAuth2 auth from folder: ${folder.name}")
                    return resolved
                }
            }
        }

        return null
    }

    /**
     * Builds a chain of folders from the target folder up to the root.
     * The first element is the immediate target folder, last is the topmost parent.
     */
    private fun buildFolderChain(
        items: List<CollectionItem>,
        targetFolderId: String
    ): List<CollectionItem.Folder> {
        val chain = mutableListOf<CollectionItem.Folder>()
        var currentId: String? = targetFolderId

        while (currentId != null) {
            val folder = findFolderById(items, currentId)
            if (folder != null) {
                chain.add(folder)
                currentId = folder.parentId
            } else {
                break
            }
        }

        return chain
    }

    /**
     * Finds a folder by ID recursively.
     */
    private fun findFolderById(items: List<CollectionItem>, folderId: String): CollectionItem.Folder? {
        for (item in items) {
            if (item.id == folderId && item is CollectionItem.Folder) {
                return item
            }
            if (item is CollectionItem.Folder) {
                val found = findFolderById(item.children, folderId)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * Resolves an OAuth2ConfigRef to an actual OAuth2Auth with a valid token.
     */
    private suspend fun resolveOAuth2ConfigRef(ref: OAuth2ConfigRef): OAuth2Auth? {
        val oauth2Service = project.service<OAuth2Service>()
        return oauth2Service.getValidAuth(ref.configId)
    }

    /**
     * Resolves any OAuth2ConfigRef within an Authentication object.
     */
    private suspend fun resolveOAuth2Ref(auth: Authentication): Authentication? {
        return when (auth) {
            is OAuth2ConfigRef -> resolveOAuth2ConfigRef(auth)
            else -> auth
        }
    }

    /**
     * Gets the effective authentication for a request, considering inheritance.
     * This is a convenience method that wraps the suspend function for synchronous callers.
     *
     * @param collectionId The collection ID
     * @param itemId The request item ID
     * @return The resolved Authentication, or null if no auth is configured
     */
    fun resolveAuthBlocking(collectionId: String, itemId: String): Authentication? {
        return runBlocking { resolveAuth(collectionId, itemId) }
    }

    /**
     * Sets the authentication for a collection.
     *
     * @param collectionId The collection ID
     * @param auth The authentication to set, or null to remove
     */
    fun setCollectionAuth(collectionId: String, auth: SerializableAuthentication?) {
        val collectionService = project.service<CollectionService>()
        val collection = collectionService.getCollection(collectionId) ?: return
        val updated = collection.copy(auth = auth).withUpdatedTimestamp()
        collectionService.updateCollection(updated)
        logger.info("Updated auth for collection: $collectionId")
    }

    /**
     * Sets the authentication for a folder.
     *
     * @param collectionId The collection ID
     * @param folderId The folder ID
     * @param auth The authentication to set, or null to remove
     */
    fun setFolderAuth(collectionId: String, folderId: String, auth: SerializableAuthentication?) {
        val collectionService = project.service<CollectionService>()
        collectionService.updateFolderAuth(collectionId, folderId, auth)
        logger.info("Updated auth for folder: $folderId in collection: $collectionId")
    }

    /**
     * Gets the authentication source description for a request item.
     * Useful for UI display of auth inheritance status.
     *
     * @param collectionId The collection ID
     * @param itemId The request item ID
     * @return A description of where the auth comes from, or null if no auth
     */
    fun getAuthSourceDescription(collectionId: String, itemId: String): String? {
        val collectionService = project.service<CollectionService>()
        val collection = collectionService.getCollection(collectionId) ?: return null
        val requestItem = collection.findItemById(itemId) as? CollectionItem.Request ?: return null

        // Check request-level
        if (requestItem.request.authentication != null) {
            return "Request"
        }

        // Check folder inheritance
        if (requestItem.parentId != null) {
            val folderChain = buildFolderChain(collection.items, requestItem.parentId)
            for (folder in folderChain) {
                if (folder.auth != null) {
                    return "Folder: ${folder.name}"
                }
            }
        }

        // Check collection-level
        if (collection.auth != null) {
            return "Collection: ${collection.name}"
        }

        return null
    }
}