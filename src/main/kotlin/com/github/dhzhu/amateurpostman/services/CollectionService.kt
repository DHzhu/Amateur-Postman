package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Service for managing request collections with persistent storage.
 *
 * This service provides CRUD operations for collections, folders, and requests,
 * allowing users to organize and save their API requests.
 *
 * @property project The IntelliJ project this service belongs to
 */
@Service(Service.Level.PROJECT)
@State(name = "CollectionService", storages = [Storage("amateur-postman-collections.xml")])
class CollectionService(private val project: Project) :
    PersistentStateComponent<CollectionState> {

    private val logger = thisLogger()

    private var state = CollectionState()
    private val listeners = mutableListOf<CollectionChangeListener>()

    override fun getState(): CollectionState = state

    override fun loadState(state: CollectionState) {
        this.state = state
        logger.info("Loaded ${state.collections.size} collections")
    }

    // ========== Collection CRUD Operations ==========

    /**
     * Creates a new collection.
     *
     * @param name The name of the collection
     * @param description Optional description
     * @return The newly created collection
     */
    fun createCollection(name: String, description: String = ""): RequestCollection {
        val collection = RequestCollection.create(name, description)
        val serializable = SerializableCollection.from(collection)
        state = state.copy(collections = state.collections + serializable)
        logger.info("Created collection: ${collection.name} (${collection.id})")
        notifyListeners()
        return collection
    }

    /**
     * Gets all collections.
     *
     * @return List of all collections
     */
    fun getCollections(): List<RequestCollection> {
        return state.collections.map { it.toCollection() }
    }

    /**
     * Gets a collection by ID.
     *
     * @param id The collection ID
     * @return The collection, or null if not found
     */
    fun getCollection(id: String): RequestCollection? {
        return state.collections.find { it.id == id }?.toCollection()
    }

    /**
     * Updates an existing collection.
     *
     * @param collection The collection to update
     * @return true if updated, false if not found
     */
    fun updateCollection(collection: RequestCollection): Boolean {
        val index = state.collections.indexOfFirst { it.id == collection.id }
        if (index >= 0) {
            val serializable = SerializableCollection.from(collection.withUpdatedTimestamp())
            val updatedList = state.collections.toMutableList()
            updatedList[index] = serializable
            state = state.copy(collections = updatedList)
            logger.info("Updated collection: ${collection.name}")
            notifyListeners()
            return true
        }
        return false
    }

    /**
     * Deletes a collection by ID.
     *
     * @param id The collection ID to delete
     * @return true if deleted, false if not found
     */
    fun deleteCollection(id: String): Boolean {
        val collection = getCollection(id)
        if (collection != null) {
            state = state.copy(collections = state.collections.filter { it.id != id })
            logger.info("Deleted collection: ${collection.name}")
            notifyListeners()
            return true
        }
        return false
    }

    /**
     * Renames a collection.
     *
     * @param id The collection ID
     * @param newName The new name
     * @return true if renamed, false if not found
     */
    fun renameCollection(id: String, newName: String): Boolean {
        val collection = getCollection(id)
        if (collection != null) {
            val updated = collection.copy(name = newName).withUpdatedTimestamp()
            return updateCollection(updated)
        }
        return false
    }

    // ========== Folder Operations ==========

    /**
     * Creates a new folder in a collection.
     *
     * @param collectionId The collection ID
     * @param name The folder name
     * @param parentId The parent folder ID (null for top-level)
     * @return The created folder item, or null if collection not found
     */
    fun createFolder(collectionId: String, name: String, parentId: String? = null): CollectionItem.Folder? {
        val collection = getCollection(collectionId) ?: return null
        val folder = CollectionItem.Folder.create(name, parentId)

        // Add folder to appropriate location
        val updatedItems = if (parentId == null) {
            collection.items + folder
        } else {
            // Insert into parent folder (requires recursive update)
            addItemToParentFolder(collection.items, parentId, folder) ?: collection.items
        }

        val updatedCollection = collection.copy(items = updatedItems).withUpdatedTimestamp()
        updateCollection(updatedCollection)
        return folder
    }

    /**
     * Deletes a folder and all its children.
     *
     * @param collectionId The collection ID
     * @param folderId The folder ID to delete
     * @return true if deleted, false if not found
     */
    fun deleteFolder(collectionId: String, folderId: String): Boolean {
        val collection = getCollection(collectionId) ?: return false

        val updatedItems = removeItemRecursive(collection.items, folderId)
        if (updatedItems.size != collection.items.size) {
            val updatedCollection = collection.copy(items = updatedItems).withUpdatedTimestamp()
            updateCollection(updatedCollection)
            return true
        }
        return false
    }

    /**
     * Renames a folder.
     *
     * @param collectionId The collection ID
     * @param folderId The folder ID
     * @param newName The new name
     * @return true if renamed, false if not found
     */
    fun renameFolder(collectionId: String, folderId: String, newName: String): Boolean {
        val collection = getCollection(collectionId) ?: return false

        val updatedItems = updateItemName(collection.items, folderId, newName)
        if (updatedItems != null) {
            val updatedCollection = collection.copy(items = updatedItems).withUpdatedTimestamp()
            updateCollection(updatedCollection)
            return true
        }
        return false
    }

    /**
     * Updates the authentication configuration for a folder.
     *
     * @param collectionId The collection ID
     * @param folderId The folder ID
     * @param auth The authentication configuration to set, or null to remove
     * @return true if updated, false if not found
     */
    fun updateFolderAuth(collectionId: String, folderId: String, auth: SerializableAuthentication?): Boolean {
        val collection = getCollection(collectionId) ?: return false

        val updatedItems = updateFolderAuthRecursive(collection.items, folderId, auth)
        if (updatedItems != null) {
            val updatedCollection = collection.copy(items = updatedItems).withUpdatedTimestamp()
            updateCollection(updatedCollection)
            logger.info("Updated auth for folder: $folderId")
            return true
        }
        return false
    }

    // ========== Request Item Operations ==========

    /**
     * Adds a request to a collection.
     *
     * @param collectionId The collection ID
     * @param request The HTTP request to save
     * @param name The name for this request
     * @param description Optional description
     * @param preRequestScript Optional pre-request script
     * @param testScript Optional test script
     * @param folderId The parent folder ID (null for top-level)
     * @return The created request item, or null if collection not found
     */
    fun addRequest(
        collectionId: String,
        request: HttpRequest,
        name: String,
        description: String = "",
        preRequestScript: String = "",
        testScript: String = "",
        folderId: String? = null
    ): CollectionItem.Request? {
        val collection = getCollection(collectionId) ?: return null
        val requestItem = CollectionItem.Request(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            request = request,
            preRequestScript = preRequestScript,
            testScript = testScript,
            parentId = folderId
        )

        // Add request to appropriate location
        val updatedItems = if (folderId == null) {
            collection.items + requestItem
        } else {
            // Insert into parent folder
            addItemToParentFolder(collection.items, folderId, requestItem) ?: collection.items
        }

        val updatedCollection = collection.copy(items = updatedItems).withUpdatedTimestamp()
        updateCollection(updatedCollection)
        return requestItem
    }

    /**
     * Updates an existing request item.
     *
     * @param collectionId The collection ID
     * @param itemId The request item ID
     * @param request The updated HTTP request
     * @param preRequestScript Optional pre-request script
     * @param testScript Optional test script
     * @return true if updated, false if not found
     */
    fun updateRequest(
        collectionId: String,
        itemId: String,
        request: HttpRequest,
        preRequestScript: String = "",
        testScript: String = ""
    ): Boolean {
        val collection = getCollection(collectionId) ?: return false

        val updatedItems = updateItemRequest(collection.items, itemId, request, preRequestScript, testScript)
        if (updatedItems != null) {
            val updatedCollection = collection.copy(items = updatedItems).withUpdatedTimestamp()
            updateCollection(updatedCollection)
            return true
        }
        return false
    }

    /**
     * Deletes a request item.
     *
     * @param collectionId The collection ID
     * @param itemId The request item ID to delete
     * @return true if deleted, false if not found
     */
    fun deleteRequest(collectionId: String, itemId: String): Boolean {
        val collection = getCollection(collectionId) ?: return false

        val updatedItems = removeItemRecursive(collection.items, itemId)
        if (updatedItems.size != collection.items.size) {
            val updatedCollection = collection.copy(items = updatedItems).withUpdatedTimestamp()
            updateCollection(updatedCollection)
            return true
        }
        return false
    }

    /**
     * Moves an item to a new parent.
     *
     * @param collectionId The collection ID
     * @param itemId The item ID to move
     * @param newParentId The new parent folder ID (null for top-level)
     * @return true if moved, false if not found
     */
    fun moveItem(collectionId: String, itemId: String, newParentId: String?): Boolean {
        val collection = getCollection(collectionId) ?: return false

        // Find and remove the item
        val item = collection.findItemById(itemId) ?: return false
        val itemsWithout = removeItemRecursive(collection.items, itemId)

        // Add to new location
        val updatedItems = if (newParentId == null) {
            itemsWithout + item
        } else {
            addItemToParentFolder(itemsWithout, newParentId, item) ?: itemsWithout
        }

        val updatedCollection = collection.copy(items = updatedItems).withUpdatedTimestamp()
        updateCollection(updatedCollection)
        return true
    }

    // ========== Helper Functions ==========

    private fun addItemToParentFolder(
        items: List<CollectionItem>,
        parentId: String,
        newItem: CollectionItem
    ): List<CollectionItem>? {
        var found = false
        val result = items.map { item ->
            if (item.id == parentId && item is CollectionItem.Folder) {
                found = true
                item.copy(children = item.children + newItem)
            } else if (item is CollectionItem.Folder) {
                val updatedChildren = addItemToParentFolder(item.children, parentId, newItem)
                if (updatedChildren != null && updatedChildren != item.children) {
                    found = true
                    item.copy(children = updatedChildren)
                } else {
                    item
                }
            } else {
                item
            }
        }

        return if (found) result else null
    }

    private fun removeItemRecursive(
        items: List<CollectionItem>,
        itemId: String
    ): List<CollectionItem> {
        return items.filter { it.id != itemId }.map { item ->
            if (item is CollectionItem.Folder) {
                item.copy(children = removeItemRecursive(item.children, itemId))
            } else {
                item
            }
        }
    }

    private fun updateItemName(
        items: List<CollectionItem>,
        itemId: String,
        newName: String
    ): List<CollectionItem>? {
        var found = false
        val result = items.map { item ->
            if (item.id == itemId) {
                found = true
                when (item) {
                    is CollectionItem.Folder -> item.copy(name = newName)
                    is CollectionItem.Request -> item.copy(name = newName)
                }
            } else if (item is CollectionItem.Folder) {
                val updatedChildren = updateItemName(item.children, itemId, newName)
                if (updatedChildren != null && updatedChildren != item.children) {
                    item.copy(children = updatedChildren)
                } else {
                    item
                }
            } else {
                item
            }
        }

        return if (found) result else null
    }

    private fun updateItemRequest(
        items: List<CollectionItem>,
        itemId: String,
        newRequest: HttpRequest,
        preRequestScript: String = "",
        testScript: String = ""
    ): List<CollectionItem>? {
        var found = false
        val result = items.map { item ->
            if (item.id == itemId && item is CollectionItem.Request) {
                found = true
                item.copy(
                    request = newRequest,
                    preRequestScript = preRequestScript,
                    testScript = testScript
                )
            } else if (item is CollectionItem.Folder) {
                val updatedChildren = updateItemRequest(item.children, itemId, newRequest, preRequestScript, testScript)
                if (updatedChildren != null && updatedChildren != item.children) {
                    item.copy(children = updatedChildren)
                } else {
                    item
                }
            } else {
                item
            }
        }

        return if (found) result else null
    }

    private fun updateFolderAuthRecursive(
        items: List<CollectionItem>,
        folderId: String,
        auth: SerializableAuthentication?
    ): List<CollectionItem>? {
        var found = false
        val result = items.map { item ->
            if (item.id == folderId && item is CollectionItem.Folder) {
                found = true
                item.copy(auth = auth)
            } else if (item is CollectionItem.Folder) {
                val updatedChildren = updateFolderAuthRecursive(item.children, folderId, auth)
                if (updatedChildren != null && updatedChildren != item.children) {
                    found = true
                    item.copy(children = updatedChildren)
                } else {
                    item
                }
            } else {
                item
            }
        }

        return if (found) result else null
    }

    // ========== Change Listeners ==========

    /**
     * Adds a listener for collection changes.
     *
     * @param listener The listener to add
     */
    fun addChangeListener(listener: CollectionChangeListener) {
        listeners.add(listener)
    }

    /**
     * Removes a change listener.
     *
     * @param listener The listener to remove
     */
    fun removeChangeListener(listener: CollectionChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.onCollectionChanged() }
    }
}

/**
 * Interface for listening to collection changes.
 */
interface CollectionChangeListener {
    /**
     * Called when collections or items change.
     */
    fun onCollectionChanged()
}
