package com.github.dhzhu.amateurpostman.models

/**
 * Represents a request collection with folders and saved requests.
 *
 * Collections allow organizing API requests into hierarchical folders
 * for better management and navigation.
 *
 * @property id Unique identifier for the collection
 * @property name Display name of the collection
 * @property description Optional description of the collection's purpose
 * @property items List of top-level items (folders or requests)
 * @property createdAt Timestamp when the collection was created
 * @property modifiedAt Timestamp when the collection was last modified
 */
data class RequestCollection(
    val id: String,
    val name: String,
    val description: String = "",
    val items: List<CollectionItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Creates a new collection with a generated UUID.
         */
        fun create(name: String, description: String = ""): RequestCollection {
            return RequestCollection(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                items = emptyList()
            )
        }
    }

    /**
     * Returns a new collection with updated modified timestamp.
     */
    fun withUpdatedTimestamp(): RequestCollection {
        return copy(modifiedAt = System.currentTimeMillis())
    }

    /**
     * Finds an item by ID recursively.
     */
    fun findItemById(itemId: String): CollectionItem? {
        return findItemByIdRecursive(itemId, items)
    }

    private fun findItemByIdRecursive(itemId: String, items: List<CollectionItem>): CollectionItem? {
        items.forEach { item ->
            if (item.id == itemId) return item
            if (item is CollectionItem.Folder) {
                val found = findItemByIdRecursive(itemId, item.children)
                if (found != null) return found
            }
        }
        return null
    }
}

/**
 * Sealed class representing items in a collection.
 *
 * Items can be either folders (containing other items) or requests (saved HTTP requests).
 */
sealed class CollectionItem {
    abstract val id: String
    abstract val name: String
    abstract val parentId: String?

    /**
     * Represents a folder that can contain other items (folders or requests).
     *
     * @property id Unique identifier for the folder
     * @property name Display name of the folder
     * @property children List of child items
     * @property parentId ID of parent folder (null for top-level)
     */
    data class Folder(
        override val id: String,
        override val name: String,
        val children: List<CollectionItem> = emptyList(),
        override val parentId: String? = null
    ) : CollectionItem() {
        companion object {
            /**
             * Creates a new folder with a generated UUID.
             */
            fun create(name: String, parentId: String? = null): Folder {
                return Folder(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    children = emptyList(),
                    parentId = parentId
                )
            }
        }

        /**
         * Returns the total count of all descendant items (recursive).
         */
        fun getTotalItemCount(): Int {
            var count = 0
            children.forEach { child ->
                count += when (child) {
                    is Folder -> 1 + child.getTotalItemCount()
                    is Request -> 1
                }
            }
            return count
        }
    }

    /**
     * Represents a saved HTTP request.
     *
     * @property id Unique identifier for the request
     * @property name Display name of the request
     * @property description Optional description of what this request does
     * @property request The actual HTTP request to send
     * @property parentId ID of parent folder (null for top-level)
     */
    data class Request(
        override val id: String,
        override val name: String,
        val description: String = "",
        val request: HttpRequest,
        override val parentId: String? = null
    ) : CollectionItem() {
        companion object {
            /**
             * Creates a new request item with a generated UUID.
             */
            fun create(name: String, request: HttpRequest, description: String = ""): Request {
                return Request(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    request = request,
                    parentId = null
                )
            }
        }

        /**
         * Returns a display string for this request.
         */
        fun getDisplayName(): String {
            val method = request.method.name
            val url = request.url.take(50)
            return "$name [$method] $url"
        }
    }
}

/**
 * Persistent state for the collection system.
 *
 * This class is serialized to XML and stored in the project.
 *
 * @property version Schema version for migration support
 * @property collections List of all collections
 */
data class CollectionState(
    val version: Int = 1,
    val collections: List<SerializableCollection> = emptyList()
)

/**
 * Serializable version of RequestCollection for XML persistence.
 *
 * @property id Unique identifier
 * @property name Display name
 * @property description Optional description
 * @property items List of serializable items
 * @property createdAt Creation timestamp
 * @property modifiedAt Last modification timestamp
 */
data class SerializableCollection(
    val id: String,
    val name: String,
    val description: String = "",
    val items: List<SerializableCollectionItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this serializable collection to a domain RequestCollection.
     */
    fun toCollection(): RequestCollection {
        return RequestCollection(
            id = id,
            name = name,
            description = description,
            items = items.map { it.toCollectionItem() },
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Creates a SerializableCollection from a domain RequestCollection.
         */
        fun from(collection: RequestCollection): SerializableCollection {
            return SerializableCollection(
                id = collection.id,
                name = collection.name,
                description = collection.description,
                items = collection.items.map { SerializableCollectionItem.from(it) },
                createdAt = collection.createdAt,
                modifiedAt = collection.modifiedAt
            )
        }
    }
}

/**
 * Serializable version of CollectionItem for XML persistence.
 *
 * @property id Unique identifier
 * @property type Item type (FOLDER or REQUEST)
 * @property name Display name
 * @property description Optional description (requests only)
 * @property request The HTTP request (requests only)
 * @property children Child items (folders only)
 * @property parentId ID of parent folder
 */
data class SerializableCollectionItem(
    val id: String,
    val type: String,
    val name: String,
    val description: String = "",
    val request: SerializableHttpRequest? = null,
    val children: List<SerializableCollectionItem> = emptyList(),
    val parentId: String? = null
) {
    /**
     * Converts this serializable item to a domain CollectionItem.
     */
    fun toCollectionItem(): CollectionItem {
        return when (type) {
            "FOLDER" -> CollectionItem.Folder(
                id = id,
                name = name,
                children = children.map { it.toCollectionItem() },
                parentId = parentId
            )
            "REQUEST" -> CollectionItem.Request(
                id = id,
                name = name,
                description = description,
                request = request?.toHttpRequest() ?: HttpRequest(
                    method = HttpMethod.GET,
                    url = "",
                    headers = emptyMap(),
                    body = null
                ),
                parentId = parentId
            )
            else -> throw IllegalArgumentException("Unknown collection item type: $type")
        }
    }

    companion object {
        /**
         * Creates a SerializableCollectionItem from a domain CollectionItem.
         */
        fun from(item: CollectionItem): SerializableCollectionItem {
            return when (item) {
                is CollectionItem.Folder -> SerializableCollectionItem(
                    id = item.id,
                    type = "FOLDER",
                    name = item.name,
                    children = item.children.map { from(it) },
                    parentId = item.parentId
                )
                is CollectionItem.Request -> SerializableCollectionItem(
                    id = item.id,
                    type = "REQUEST",
                    name = item.name,
                    description = item.description,
                    request = SerializableHttpRequest.from(item.request),
                    parentId = item.parentId
                )
            }
        }
    }
}

/**
 * Serializable version of HttpRequest for persistence.
 *
 * This wraps the existing HttpRequest with additional metadata if needed.
 */
data class SerializableHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String?,
    val contentType: String?
) {
    fun toHttpRequest(): HttpRequest {
        return HttpRequest(
            method = HttpMethod.valueOf(method),
            url = url,
            headers = headers,
            body = body,
            contentType = contentType
        )
    }

    companion object {
        fun from(request: HttpRequest): SerializableHttpRequest {
            return SerializableHttpRequest(
                method = request.method.name,
                url = request.url,
                headers = request.headers,
                body = request.body,
                contentType = request.contentType
            )
        }
    }
}
