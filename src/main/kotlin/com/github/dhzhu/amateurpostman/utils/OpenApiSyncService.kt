package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import java.util.UUID

/**
 * Service for synchronizing collections with their linked OpenAPI specifications.
 *
 * Supports:
 * - Binding a collection to an OpenAPI source
 * - Incremental sync (preserves user scripts)
 * - Change detection between local and remote
 */
object OpenApiSyncService {

    /**
     * Result of a sync operation.
     *
     * @property collection The synchronized collection
     * @property added Number of items added
     * @property updated Number of items updated
     * @property removed Number of items removed
     * @property preserved Number of items preserved (with user modifications)
     * @property warnings Warning messages during sync
     * @property error Error message if sync failed
     */
    data class SyncResult(
        val collection: RequestCollection?,
        val added: Int = 0,
        val updated: Int = 0,
        val removed: Int = 0,
        val preserved: Int = 0,
        val warnings: List<String> = emptyList(),
        val error: String? = null
    ) {
        val isSuccess: Boolean get() = collection != null
        val hasChanges: Boolean get() = added > 0 || updated > 0 || removed > 0
    }

    /**
     * Represents a detected change between local and remote.
     */
    sealed class Change {
        abstract val path: String
        abstract val method: PathItem.HttpMethod

        data class Added(override val path: String, override val method: PathItem.HttpMethod, val operation: Operation) : Change()
        data class Updated(override val path: String, override val method: PathItem.HttpMethod, val operation: Operation) : Change()
        data class Removed(override val path: String, override val method: PathItem.HttpMethod) : Change()
    }

    /**
     * Binds a collection to an OpenAPI source.
     *
     * @param collection The collection to bind
     * @param openApiSource The OpenAPI source URL or file path
     * @return The collection with the source bound
     */
    fun bindCollection(collection: RequestCollection, openApiSource: String): RequestCollection {
        return collection.copy(openApiSource = openApiSource)
    }

    /**
     * Synchronizes a collection with its linked OpenAPI source.
     *
     * @param collection The collection to sync
     * @param openApiContent The current OpenAPI content (parsed)
     * @param strategy The sync strategy to use
     * @return SyncResult containing the updated collection
     */
    fun syncCollection(
        collection: RequestCollection,
        openApiContent: OpenAPI,
        strategy: SyncStrategy = SyncStrategy.INCREMENTAL
    ): SyncResult {
        val warnings = mutableListOf<String>()

        try {
            when (strategy) {
                SyncStrategy.FULL_REPLACE -> {
                    return fullReplace(collection, openApiContent, warnings)
                }
                SyncStrategy.INCREMENTAL -> {
                    return incrementalSync(collection, openApiContent, warnings)
                }
            }
        } catch (e: Exception) {
            return SyncResult(
                collection = null,
                error = "Sync failed: ${e.message}",
                warnings = warnings
            )
        }
    }

    /**
     * Detects changes between local collection and OpenAPI spec.
     *
     * @param collection The local collection
     * @param openApi The OpenAPI specification
     * @return List of detected changes
     */
    fun detectChanges(collection: RequestCollection, openApi: OpenAPI): List<Change> {
        val changes = mutableListOf<Change>()
        val localEndpoints = extractLocalEndpoints(collection)
        val remoteEndpoints = extractRemoteEndpoints(openApi)

        // Detect added and updated
        remoteEndpoints.forEach { (key, remoteOp) ->
            val localOp = localEndpoints[key]
            if (localOp == null) {
                changes.add(Change.Added(key.first, key.second, remoteOp))
            } else {
                // Check if operation has changed (simplified: check summary)
                if (remoteOp.summary != localOp.operation.summary) {
                    changes.add(Change.Updated(key.first, key.second, remoteOp))
                }
            }
        }

        // Detect removed
        localEndpoints.forEach { (key, localOp) ->
            if (!remoteEndpoints.containsKey(key)) {
                changes.add(Change.Removed(key.first, key.second))
            }
        }

        return changes
    }

    /**
     * Full replace strategy - replaces all items with OpenAPI content.
     */
    private fun fullReplace(
        collection: RequestCollection,
        openApi: OpenAPI,
        warnings: MutableList<String>
    ): SyncResult {
        val importResult = OpenApiImporter.import(openApi)

        if (!importResult.isSuccess) {
            return SyncResult(
                collection = null,
                error = importResult.error,
                warnings = warnings + importResult.warnings
            )
        }

        val newCollection = importResult.collection!!.copy(
            id = collection.id,
            openApiSource = collection.openApiSource,
            createdAt = collection.createdAt
        )

        val oldCount = countRequests(collection.items)
        val newCount = countRequests(newCollection.items)

        return SyncResult(
            collection = newCollection,
            added = newCount,
            removed = oldCount,
            warnings = warnings + importResult.warnings
        )
    }

    /**
     * Incremental sync - preserves user modifications (scripts).
     */
    private fun incrementalSync(
        collection: RequestCollection,
        openApi: OpenAPI,
        warnings: MutableList<String>
    ): SyncResult {
        val changes = detectChanges(collection, openApi)
        var added = 0
        var updated = 0
        var removed = 0
        var preserved = 0

        // Build a map of existing items by operation key
        val existingItems = mutableMapOf<Pair<String, PathItem.HttpMethod>, CollectionItem.Request>()
        flattenRequests(collection.items).forEach { request ->
            val key = extractOperationKey(request)
            if (key != null) {
                existingItems[key] = request
            }
        }

        // Process remote endpoints
        val newItems = mutableListOf<CollectionItem>()
        val tagGroups = mutableMapOf<String, MutableList<CollectionItem.Request>>()

        openApi.paths?.forEach { (path, pathItem) ->
            pathItem?.readOperationsMap()?.forEach { (method, operation) ->
                if (operation != null) {
                    val key = path to method
                    val existing = existingItems[key]

                    val request = if (existing != null) {
                        // Check if needs update
                        val needsUpdate = changes.any {
                            it is Change.Updated && it.path == path && it.method == method
                        }

                        if (needsUpdate) {
                            // Update but preserve scripts
                            val updatedRequest = createRequestFromOperation(openApi, path, method, operation, warnings)
                            updatedRequest?.copy(
                                id = existing.id,
                                preRequestScript = existing.preRequestScript,
                                testScript = existing.testScript
                            ) ?: existing
                        } else {
                            // Preserve existing
                            preserved++
                            existing
                        }
                    } else {
                        // New endpoint
                        added++
                        createRequestFromOperation(openApi, path, method, operation, warnings)
                    }

                    if (request != null) {
                        val tags = operation.tags?.takeIf { it.isNotEmpty() } ?: listOf("Default")
                        tags.forEach { tag ->
                            tagGroups.getOrPut(tag) { mutableListOf() }.add(request)
                        }
                    }
                }
            }
        }

        // Count removed
        removed = changes.count { it is Change.Removed }
        updated = changes.count { it is Change.Updated }

        // Build folder structure
        val items = tagGroups.map { (tag, requests) ->
            CollectionItem.Folder(
                id = UUID.randomUUID().toString(),
                name = tag,
                children = requests
            )
        }

        val syncedCollection = collection.copy(
            items = items,
            modifiedAt = System.currentTimeMillis()
        )

        return SyncResult(
            collection = syncedCollection,
            added = added,
            updated = updated,
            removed = removed,
            preserved = preserved,
            warnings = warnings
        )
    }

    /**
     * Creates a request from an OpenAPI operation.
     */
    private fun createRequestFromOperation(
        openApi: OpenAPI,
        path: String,
        method: PathItem.HttpMethod,
        operation: Operation,
        warnings: MutableList<String>
    ): CollectionItem.Request? {
        // Use OpenApiImporter's conversion logic
        val tempResult = OpenApiImporter.import(openApi)
        if (!tempResult.isSuccess) return null

        // Find the matching request in the imported collection
        return findRequestByPathAndMethod(tempResult.collection!!.items, path, method)
    }

    /**
     * Finds a request by path and method in the collection items.
     */
    private fun findRequestByPathAndMethod(
        items: List<CollectionItem>,
        path: String,
        method: PathItem.HttpMethod
    ): CollectionItem.Request? {
        for (item in items) {
            when (item) {
                is CollectionItem.Folder -> {
                    val found = findRequestByPathAndMethod(item.children, path, method)
                    if (found != null) return found
                }
                is CollectionItem.Request -> {
                    if (item.request.url.contains(path) && item.request.method.name == method.name) {
                        return item
                    }
                }
            }
        }
        return null
    }

    /**
     * Extracts local endpoints from a collection.
     */
    private fun extractLocalEndpoints(collection: RequestCollection): Map<Pair<String, PathItem.HttpMethod>, LocalOperation> {
        val endpoints = mutableMapOf<Pair<String, PathItem.HttpMethod>, LocalOperation>()

        flattenRequests(collection.items).forEach { request ->
            val key = extractOperationKey(request)
            if (key != null) {
                endpoints[key] = LocalOperation(
                    path = key.first,
                    method = key.second,
                    operation = StubOperation(request.name)
                )
            }
        }

        return endpoints
    }

    /**
     * Extracts remote endpoints from an OpenAPI spec.
     */
    private fun extractRemoteEndpoints(openApi: OpenAPI): Map<Pair<String, PathItem.HttpMethod>, Operation> {
        val endpoints = mutableMapOf<Pair<String, PathItem.HttpMethod>, Operation>()

        openApi.paths?.forEach { (path, pathItem) ->
            pathItem?.readOperationsMap()?.forEach { (method, operation) ->
                if (operation != null) {
                    endpoints[path to method] = operation
                }
            }
        }

        return endpoints
    }

    /**
     * Extracts an operation key from a request (path + method).
     */
    private fun extractOperationKey(request: CollectionItem.Request): Pair<String, PathItem.HttpMethod>? {
        val url = request.request.url
        val method = try {
            PathItem.HttpMethod.valueOf(request.request.method.name)
        } catch (e: IllegalArgumentException) {
            return null
        }

        // Extract path from URL (remove base URL)
        val path = extractPath(url)
        return path to method
    }

    /**
     * Extracts the path portion from a URL.
     */
    private fun extractPath(url: String): String {
        // Remove protocol and host
        val withoutProtocol = url.substringAfter("://", url)
        val path = withoutProtocol.substringAfter("/", "")
        return "/$path".substringBefore("?")
    }

    /**
     * Flattens all requests in a collection.
     */
    private fun flattenRequests(items: List<CollectionItem>): List<CollectionItem.Request> {
        val requests = mutableListOf<CollectionItem.Request>()
        items.forEach { item ->
            when (item) {
                is CollectionItem.Folder -> requests.addAll(flattenRequests(item.children))
                is CollectionItem.Request -> requests.add(item)
            }
        }
        return requests
    }

    /**
     * Counts the number of requests in a collection.
     */
    private fun countRequests(items: List<CollectionItem>): Int {
        return items.sumOf { item ->
            when (item) {
                is CollectionItem.Folder -> countRequests(item.children)
                is CollectionItem.Request -> 1
            }
        }
    }

    /**
     * Local operation representation for change detection.
     */
    private data class LocalOperation(
        val path: String,
        val method: PathItem.HttpMethod,
        val operation: StubOperation
    )

    /**
     * Stub operation for local comparison.
     */
    private data class StubOperation(val summary: String?)

    /**
     * Sync strategy enum.
     */
    enum class SyncStrategy {
        /** Full replace - all items are replaced with OpenAPI content */
        FULL_REPLACE,
        /** Incremental - preserves user scripts and only updates changed items */
        INCREMENTAL
    }
}