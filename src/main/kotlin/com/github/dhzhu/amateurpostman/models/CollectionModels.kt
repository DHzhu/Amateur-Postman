package com.github.dhzhu.amateurpostman.models

/**
 * Serializable representation of Authentication for persistence.
 * Supports all authentication types via a type discriminator.
 */
data class SerializableAuthentication(
    var type: String = "NONE",  // "NONE", "BASIC", "BEARER", "API_KEY", "OAUTH2_CONFIG"
    var username: String? = null,      // For Basic Auth
    var password: String? = null,      // For Basic Auth
    var token: String? = null,         // For Bearer Token
    var apiKey: String? = null,        // For API Key
    var apiValue: String? = null,      // For API Key
    var apiKeyLocation: String? = null, // For API Key: "HEADER" or "QUERY"
    var oauth2ConfigId: String? = null // For OAuth2 (reference to OAuth2Service config)
) {
    /**
     * Converts this serializable authentication to a domain Authentication object.
     * Note: OAuth2 auth requires the OAuth2Service to resolve the config.
     */
    fun toAuthentication(): Authentication? {
        return when (type) {
            "NONE" -> NoAuth
            "BASIC" -> {
                val u = username
                val p = password
                if (u != null && p != null) {
                    BasicAuth(u, p)
                } else null
            }
            "BEARER" -> {
                val t = token
                if (t != null) {
                    BearerToken(t)
                } else null
            }
            "API_KEY" -> {
                val k = apiKey
                val v = apiValue
                if (k != null && v != null) {
                    val location = when (apiKeyLocation) {
                        "QUERY" -> ApiKeyAuth.ApiKeyLocation.QUERY
                        else -> ApiKeyAuth.ApiKeyLocation.HEADER
                    }
                    ApiKeyAuth(k, v, location)
                } else null
            }
            "OAUTH2_CONFIG" -> {
                // OAuth2 requires runtime resolution via OAuth2Service
                // Return a placeholder that indicates OAuth2 config ID
                OAuth2ConfigRef(oauth2ConfigId ?: return null)
            }
            else -> null
        }
    }

    companion object {
        /**
         * Creates a SerializableAuthentication from a domain Authentication object.
         */
        fun from(auth: Authentication?): SerializableAuthentication? {
            return when (auth) {
                null, NoAuth -> SerializableAuthentication(type = "NONE")
                is BasicAuth -> SerializableAuthentication(
                    type = "BASIC",
                    username = auth.username,
                    password = auth.password
                )
                is BearerToken -> SerializableAuthentication(
                    type = "BEARER",
                    token = auth.token
                )
                is ApiKeyAuth -> SerializableAuthentication(
                    type = "API_KEY",
                    apiKey = auth.key,
                    apiValue = auth.value,
                    apiKeyLocation = auth.addTo.name
                )
                is OAuth2ConfigRef -> SerializableAuthentication(
                    type = "OAUTH2_CONFIG",
                    oauth2ConfigId = auth.configId
                )
                is OAuth2Auth -> {
                    // For OAuth2Auth with inline config, we can't persist directly
                    // This should be converted to OAuth2ConfigRef before persistence
                    null
                }
            }
        }
    }
}

/**
 * Reference to an OAuth2 configuration stored in OAuth2Service.
 * Used for collection/folder-level OAuth2 authentication.
 */
data class OAuth2ConfigRef(
    val configId: String
) : Authentication {
    // This is a placeholder - actual auth headers are resolved via OAuth2Service
    override fun toHeaders(): Map<String, String> = emptyMap()
}

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
 * @property variables List of collection-level variables
 * @property auth Authentication configuration for this collection (inherited by children)
 */
data class RequestCollection(
    val id: String,
    val name: String,
    val description: String = "",
    val items: List<CollectionItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val variables: List<Variable> = emptyList(),
    val openApiSource: String? = null,  // URL or file path to the linked OpenAPI spec
    val auth: SerializableAuthentication? = null  // Collection-level auth
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
     * @property auth Authentication configuration for this folder (inherited by children)
     */
    data class Folder(
        override val id: String,
        override val name: String,
        val children: List<CollectionItem> = emptyList(),
        override val parentId: String? = null,
        val auth: SerializableAuthentication? = null  // Folder-level auth
    ) : CollectionItem() {
        companion object {
            /**
             * Creates a new folder with a generated UUID.
             */
            fun create(name: String, parentId: String? = null, auth: SerializableAuthentication? = null): Folder {
                return Folder(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    children = emptyList(),
                    parentId = parentId,
                    auth = auth
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
     * @property preRequestScript Optional script to execute before sending the request
     * @property testScript Optional script to execute after receiving the response
     * @property parentId ID of parent folder (null for top-level)
     */
    data class Request(
        override val id: String,
        override val name: String,
        val description: String = "",
        val request: HttpRequest,
        val preRequestScript: String = "",
        val testScript: String = "",
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

        /**
         * Checks if this request has any scripts defined.
         */
        fun hasScripts(): Boolean {
            return preRequestScript.isNotBlank() || testScript.isNotBlank()
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
    var version: Int = 1,
    var collections: List<SerializableCollection> = emptyList()
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
 * @property variables List of collection-level variables
 * @property openApiSource URL or file path to the linked OpenAPI specification
 * @property auth Collection-level authentication configuration
 */
data class SerializableCollection(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var items: List<SerializableCollectionItem> = emptyList(),
    var createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis(),
    var variables: List<SerializableVariable> = emptyList(),
    var openApiSource: String? = null,
    var auth: SerializableAuthentication? = null
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
            modifiedAt = modifiedAt,
            variables = variables.map { it.toVariable() },
            openApiSource = openApiSource,
            auth = auth
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
                modifiedAt = collection.modifiedAt,
                variables = collection.variables.map { SerializableVariable.from(it) },
                openApiSource = collection.openApiSource,
                auth = collection.auth
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
 * @property preRequestScript Script to execute before the request (requests only)
 * @property testScript Script to execute after the response (requests only)
 * @property children Child items (folders only)
 * @property parentId ID of parent folder
 * @property auth Folder-level authentication configuration (folders only)
 */
data class SerializableCollectionItem(
    var id: String = "",
    var type: String = "",
    var name: String = "",
    var description: String = "",
    var request: SerializableHttpRequest? = null,
    var preRequestScript: String = "",
    var testScript: String = "",
    var children: List<SerializableCollectionItem> = emptyList(),
    var parentId: String? = null,
    var auth: SerializableAuthentication? = null
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
                parentId = parentId,
                auth = auth
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
                preRequestScript = preRequestScript,
                testScript = testScript,
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
                    parentId = item.parentId,
                    auth = item.auth
                )
                is CollectionItem.Request -> SerializableCollectionItem(
                    id = item.id,
                    type = "REQUEST",
                    name = item.name,
                    description = item.description,
                    request = SerializableHttpRequest.from(item.request),
                    preRequestScript = item.preRequestScript,
                    testScript = item.testScript,
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
    var method: String = "GET",
    var url: String = "",
    var headers: Map<String, String> = emptyMap(),
    var body: String? = null,
    var bodyType: String? = null,  // "JSON", "XML", "TEXT", "HTML", "JAVASCRIPT"
    var authentication: SerializableAuthentication? = null,
    var multipartData: String? = null  // JSON-serialized List<SerializableMultipartPart>
) {
    fun toHttpRequest(): HttpRequest {
        val type = bodyType?.let { typeName ->
            BodyType.entries.find { it.name == typeName } ?: BodyType.JSON
        } ?: BodyType.JSON

        val multipartParts = if (type == BodyType.MULTIPART && !multipartData.isNullOrBlank()) {
            deserializeMultipartParts(multipartData!!)
        } else null

        return HttpRequest(
            method = try { HttpMethod.valueOf(method) } catch (_: IllegalArgumentException) { HttpMethod.GET },
            url = url,
            headers = headers,
            body = if (type == BodyType.MULTIPART && multipartParts != null) {
                HttpBody(content = "", type = BodyType.MULTIPART, multipartData = multipartParts)
            } else {
                body?.let { HttpBody(it, type) }
            },
            authentication = authentication?.toAuthentication()
        )
    }

    companion object {
        fun from(request: HttpRequest): SerializableHttpRequest {
            val multipartJson = if (request.body?.type == BodyType.MULTIPART && request.body.multipartData != null) {
                serializeMultipartParts(request.body.multipartData!!)
            } else null

            return SerializableHttpRequest(
                method = request.method.name,
                url = request.url,
                headers = request.headers,
                body = request.body?.content,
                bodyType = request.body?.type?.name,
                authentication = SerializableAuthentication.from(request.authentication),
                multipartData = multipartJson
            )
        }

        private fun serializeMultipartParts(parts: List<MultipartPart>): String {
            val mapper = com.github.dhzhu.amateurpostman.services.JsonService.compactMapper
            val list = parts.map { part ->
                when (part) {
                    is MultipartPart.TextField -> mapOf(
                        "type" to "text",
                        "key" to part.key,
                        "value" to part.value,
                        "contentType" to (part.contentType ?: ""),
                        "description" to part.description
                    )
                    is MultipartPart.FileField -> mapOf(
                        "type" to "file",
                        "key" to part.key,
                        "filePath" to part.filePath,
                        "fileName" to part.fileName,
                        "contentType" to (part.contentType ?: ""),
                        "description" to part.description
                    )
                }
            }
            return mapper.writeValueAsString(list)
        }

        private fun deserializeMultipartParts(json: String): List<MultipartPart>? {
            return try {
                val mapper = com.github.dhzhu.amateurpostman.services.JsonService.mapper
                val node = mapper.readTree(json)
                if (!node.isArray) return null
                node.mapNotNull { element ->
                    when (element.get("type")?.asText()) {
                        "text" -> MultipartPart.TextField(
                            key = element.get("key")?.asText() ?: "",
                            value = element.get("value")?.asText() ?: "",
                            contentType = element.get("contentType")?.asText()?.ifBlank { null },
                            description = element.get("description")?.asText() ?: ""
                        )
                        "file" -> MultipartPart.FileField(
                            key = element.get("key")?.asText() ?: "",
                            filePath = element.get("filePath")?.asText() ?: "",
                            fileName = element.get("fileName")?.asText() ?: "",
                            contentType = element.get("contentType")?.asText()?.ifBlank { null },
                            description = element.get("description")?.asText() ?: ""
                        )
                        else -> null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
