package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.File
import java.io.IOException

/**
 * Importer for Postman Collection v2.1 format.
 *
 * Supports importing Postman collections exported from Postman and
 * converting them to the internal collection format.
 *
 * Postman Collection v2.1 schema:
 * https://schema.getpostman.com/json/collection/v2.1.0/collection-v2.1.0.json
 */
object PostmanImporter {

    private val gson = Gson()

    /**
     * Data class representing Postman Collection v2.1 JSON structure.
     */
    private data class PostmanCollection(
        val info: PostmanInfo,
        val item: List<PostmanItem>?
    )

    private data class PostmanInfo(
        val name: String,
        val description: String? = null,
        val _postman_id: String? = null
    )

    private data class PostmanItem(
        val name: String,
        val item: List<PostmanItem>? = null,
        val request: PostmanRequest? = null,
        val description: String? = null
    )

    private data class PostmanRequest(
        val method: String,
        val header: List<PostmanHeader>? = null,
        val body: PostmanBody? = null,
        val url: PostmanUrl,
        val description: String? = null
    )

    private data class PostmanHeader(
        val key: String,
        val value: String,
        val description: String? = null
    )

    private data class PostmanBody(
        val mode: String?,
        val raw: String? = null,
        val formdata: List<PostmanFormData>? = null,
        val urlencoded: List<PostmanUrlEncoded>? = null
    )

    private data class PostmanFormData(
        val key: String,
        val value: String? = null,
        val type: String? = null,
        val disabled: Boolean? = false
    )

    private data class PostmanUrlEncoded(
        val key: String,
        val value: String? = null,
        val description: String? = null,
        val disabled: Boolean? = false
    )

    private data class PostmanUrl(
        val raw: String?,
        val protocol: String? = null,
        val host: List<String>? = null,
        val path: List<String>? = null,
        val query: List<PostmanQueryParam>? = null,
        val variable: List<PostmanVariable>? = null
    )

    private data class PostmanQueryParam(
        val key: String,
        val value: String? = null,
        val description: String? = null,
        val disabled: Boolean? = false
    )

    private data class PostmanVariable(
        val key: String,
        val value: String? = null,
        val description: String? = null
    )

    /**
     * Import result containing either the collection or error information.
     */
    data class ImportResult(
        val collection: RequestCollection?,
        val warnings: List<String>,
        val error: String? = null
    ) {
        companion object {
            fun success(collection: RequestCollection, warnings: List<String> = emptyList()) =
                ImportResult(collection, warnings, null)

            fun error(message: String) =
                ImportResult(null, emptyList(), message)
        }

        val isSuccess: Boolean get() = collection != null
    }

    /**
     * Imports a Postman Collection from a JSON file.
     *
     * @param file The Postman collection JSON file
     * @return ImportResult containing the collection or error details
     */
    fun importFromFile(file: File): ImportResult {
        try {
            val jsonContent = file.readText()
            return importFromJson(jsonContent)
        } catch (e: IOException) {
            return ImportResult.error("Failed to read file: ${e.message}")
        }
    }

    /**
     * Imports a Postman Collection from a JSON string.
     *
     * @param jsonContent The Postman collection JSON content
     * @return ImportResult containing the collection or error details
     */
    fun importFromJson(jsonContent: String): ImportResult {
        val warnings = mutableListOf<String>()

        try {
            val postmanCollection = try {
                gson.fromJson(jsonContent, PostmanCollection::class.java)
            } catch (e: JsonSyntaxException) {
                return ImportResult.error("Invalid JSON format: ${e.message}")
            }

            // Validate collection structure
            if (postmanCollection.info.name.isBlank()) {
                return ImportResult.error("Collection name is missing")
            }

            // Convert items
            val items = mutableListOf<CollectionItem>()
            postmanCollection.item?.forEach { postmanItem ->
                val convertedItems = convertPostmanItem(postmanItem, warnings)
                items.addAll(convertedItems)
            }

            val collection = RequestCollection(
                id = java.util.UUID.randomUUID().toString(),
                name = postmanCollection.info.name,
                description = postmanCollection.info.description ?: "",
                items = items,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )

            return ImportResult.success(collection, warnings)

        } catch (e: Exception) {
            return ImportResult.error("Failed to parse collection: ${e.message}")
        }
    }

    /**
     * Recursively converts Postman items to collection items.
     */
    private fun convertPostmanItem(
        postmanItem: PostmanItem,
        warnings: MutableList<String>,
        parentId: String? = null
    ): List<CollectionItem> {
        return when {
            // Folder (has sub-items)
            postmanItem.item != null && postmanItem.request == null -> {
                val folder = CollectionItem.Folder(
                    id = java.util.UUID.randomUUID().toString(),
                    name = postmanItem.name,
                    parentId = parentId,
                    children = emptyList()
                )

                // Recursively convert children
                val children = mutableListOf<CollectionItem>()
                postmanItem.item.forEach { childItem ->
                    val convertedItems = convertPostmanItem(childItem, warnings, folder.id)
                    children.addAll(convertedItems)
                }

                listOf(folder.copy(children = children))
            }

            // Request (has request object)
            postmanItem.request != null -> {
                val request = try {
                    convertPostmanRequest(postmanItem.request, warnings)
                } catch (e: Exception) {
                    warnings.add("Failed to convert request '${postmanItem.name}': ${e.message}")
                    return emptyList()
                }

                val requestItem = CollectionItem.Request(
                    id = java.util.UUID.randomUUID().toString(),
                    name = postmanItem.name,
                    request = request,
                    parentId = parentId,
                    description = postmanItem.description ?: ""
                )

                listOf(requestItem)
            }

            // Unknown item type
            else -> {
                warnings.add("Skipped item '${postmanItem.name}': no request or sub-items")
                emptyList()
            }
        }
    }

    /**
     * Converts a Postman request to an HttpRequest.
     */
    private fun convertPostmanRequest(
        postmanRequest: PostmanRequest,
        warnings: MutableList<String>
    ): HttpRequest {
        // Convert method
        val method = try {
            HttpMethod.valueOf(postmanRequest.method.uppercase())
        } catch (e: IllegalArgumentException) {
            warnings.add("Unknown HTTP method '${postmanRequest.method}', defaulting to GET")
            HttpMethod.GET
        }

        // Build URL
        val url = buildUrl(postmanRequest.url, warnings)

        // Convert headers
        val headers = postmanRequest.header?.associate { header ->
            header.key to (header.value ?: "")
        } ?: emptyMap()

        // Convert body with type detection
        val contentType = headers["Content-Type"]
        val bodyType = BodyType.fromMimeType(contentType)
        val bodyContent = convertBody(postmanRequest.body, warnings)
        val body = bodyContent?.let { HttpBody(it, bodyType) }

        return HttpRequest(
            method = method,
            url = url,
            headers = headers,
            body = body
        )
    }

    /**
     * Builds a URL from Postman URL structure.
     */
    private fun buildUrl(postmanUrl: PostmanUrl, warnings: MutableList<String>): String {
        // Build from components when available to properly handle disabled parameters
        val hasStructuredComponents = postmanUrl.host != null && postmanUrl.host!!.isNotEmpty()

        var url = if (hasStructuredComponents) {
            val protocol = postmanUrl.protocol ?: "https"
            val host = postmanUrl.host?.joinToString(".") ?: ""
            val path = postmanUrl.path?.joinToString("/") ?: ""

            var builtUrl = "$protocol://$host"
            if (path.isNotBlank()) {
                builtUrl += "/$path"
            }
            builtUrl
        } else {
            postmanUrl.raw ?: ""
        }

        // Add query parameters (only non-disabled ones)
        // Only add if not already present in the URL
        val queryParams = postmanUrl.query
            ?.filter { !(it.disabled == true) && it.key.isNotBlank() }
            ?.map { "${it.key}=${it.value ?: ""}" }
            ?.joinToString("&")

        if (!queryParams.isNullOrBlank() && !url.contains("?")) {
            url += "?$queryParams"
        } else if (!queryParams.isNullOrBlank()) {
            // URL already has query string, append
            url += "&$queryParams"
        }

        // Process variables (replace {{variable}} with actual values if available)
        val urlWithVars = postmanUrl.variable?.fold(url) { acc, variable ->
            val placeholder = "{{${variable.key}}}"
            val value = variable.value ?: ""
            acc.replace(placeholder, value)
        } ?: url

        return urlWithVars
    }

    /**
     * Converts Postman body to request body string.
     */
    private fun convertBody(postmanBody: PostmanBody?, warnings: MutableList<String>): String? {
        if (postmanBody == null) return null

        return when (postmanBody.mode) {
            "raw" -> postmanBody.raw
            "formdata" -> {
                // Build form-data as multipart string
                postmanBody.formdata
                    ?.filter { !(it.disabled == true) && it.key.isNotBlank() }
                    ?.joinToString("&") { field ->
                        val value = field.value ?: ""
                        "${field.key}=$value"
                    }
            }
            "urlencoded" -> {
                // Build URL-encoded form
                postmanBody.urlencoded
                    ?.filter { !(it.disabled == true) && it.key.isNotBlank() }
                    ?.joinToString("&") { param ->
                        val value = param.value ?: ""
                        "${param.key}=$value"
                    }
            }
            else -> {
                warnings.add("Unsupported body mode '${postmanBody.mode}'")
                null
            }
        }
    }

    /**
     * Validates if a JSON file is a valid Postman Collection.
     *
     * @param file The file to validate
     * @return true if valid, false otherwise
     */
    fun isValidCollection(file: File): Boolean {
        return try {
            val jsonContent = file.readText()
            val collection = gson.fromJson(jsonContent, PostmanCollection::class.java)
            collection.info.name.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}
