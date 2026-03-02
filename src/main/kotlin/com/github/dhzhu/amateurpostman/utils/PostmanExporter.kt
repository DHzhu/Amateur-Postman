package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Exporter for Postman Collection v2.1 format.
 *
 * Converts internal collections to Postman Collection v2.1 JSON format
 * for export and sharing.
 *
 * Postman Collection v2.1 schema:
 * https://schema.getpostman.com/json/collection/v2.1.0/collection-v2.1.0.json
 */
object PostmanExporter {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Export result containing either the JSON string or error information.
     */
    data class ExportResult(
        val json: String?,
        val warnings: List<String>,
        val error: String? = null
    ) {
        companion object {
            fun success(json: String, warnings: List<String> = emptyList()) =
                ExportResult(json, warnings, null)

            fun error(message: String) =
                ExportResult(null, emptyList(), message)
        }

        val isSuccess: Boolean get() = json != null
    }

    /**
     * Exports a collection to Postman Collection v2.1 JSON format.
     *
     * @param collection The collection to export
     * @return ExportResult containing the JSON or error details
     */
    fun exportCollection(collection: RequestCollection): ExportResult {
        val warnings = mutableListOf<String>()

        try {
            val postmanCollection = convertToPostmanCollection(collection, warnings)
            val json = gson.toJson(postmanCollection)
            return ExportResult.success(json, warnings)
        } catch (e: Exception) {
            return ExportResult.error("Failed to export collection: ${e.message}")
        }
    }

    /**
     * Exports a collection and saves it to a file.
     *
     * @param collection The collection to export
     * @param file The file to save to
     * @return ExportResult containing the JSON or error details
     */
    fun exportToFile(collection: RequestCollection, file: File): ExportResult {
        val exportResult = exportCollection(collection)

        if (!exportResult.isSuccess) {
            return exportResult
        }

        return try {
            file.writeText(exportResult.json ?: "")
            exportResult
        } catch (e: Exception) {
            ExportResult.error("Failed to write to file: ${e.message}")
        }
    }

    /**
     * Converts an internal collection to Postman Collection format.
     */
    private fun convertToPostmanCollection(
        collection: RequestCollection,
        warnings: MutableList<String>
    ): PostmanCollection {
        val items = collection.items.mapNotNull { item ->
            convertCollectionItem(item, warnings)
        }

        return PostmanCollection(
            info = PostmanInfo(
                name = collection.name,
                description = collection.description.ifEmpty { null },
                _postman_id = collection.id
            ),
            item = if (items.isEmpty()) null else items
        )
    }

    /**
     * Recursively converts a collection item to Postman format.
     */
    private fun convertCollectionItem(
        item: CollectionItem,
        warnings: MutableList<String>
    ): PostmanItem? {
        return when (item) {
            is CollectionItem.Folder -> {
                val children = item.children.mapNotNull { child ->
                    convertCollectionItem(child, warnings)
                }

                if (children.isEmpty()) {
                    warnings.add("Skipping empty folder '${item.name}'")
                    return null
                }

                PostmanItem(
                    name = item.name,
                    item = children,
                    request = null,
                    description = null
                )
            }
            is CollectionItem.Request -> {
                val postmanRequest = convertHttpRequest(item.request, warnings)

                PostmanItem(
                    name = item.name,
                    item = null,
                    request = postmanRequest,
                    description = item.description.ifEmpty { null }
                )
            }
        }
    }

    /**
     * Converts an HttpRequest to Postman Request format.
     */
    private fun convertHttpRequest(
        request: HttpRequest,
        warnings: MutableList<String>
    ): PostmanRequest {
        // Parse URL into components (basic parsing)
        val urlComponents = parseUrl(request.url, warnings)

        // Convert headers
        val headers = request.headers.map { (key, value) ->
            PostmanHeader(key = key, value = value)
        }

        // Convert body
        val body = convertBody(request.body, warnings)

        return PostmanRequest(
            method = request.method.name,
            header = if (headers.isEmpty()) null else headers,
            body = body,
            url = urlComponents,
            description = null
        )
    }

    /**
     * Parses a URL string into PostmanUrl components.
     */
    private fun parseUrl(url: String, warnings: MutableList<String>): PostmanUrl {
        try {
            // Basic URL parsing
            val queryStringIndex = url.indexOf('?')
            val baseUrl = if (queryStringIndex >= 0) {
                url.substring(0, queryStringIndex)
            } else {
                url
            }

            // Extract protocol
            val protocol = when {
                url.startsWith("https://") -> "https"
                url.startsWith("http://") -> "http"
                else -> null
            }

            // Extract host (simple extraction)
            val withoutProtocol = if (protocol != null) {
                url.substringAfter("://")
            } else {
                url
            }
            val hostPart = withoutProtocol.substringBefore("/").substringBefore("?")
            val host = hostPart.split('.')

            // Extract path
            val pathPart = if (queryStringIndex >= 0) {
                baseUrl.substringAfter("://").substringAfter("/").takeIf { it != baseUrl.substringAfter("://") }
            } else {
                url.substringAfter("://").substringAfter("/").takeIf { it != url.substringAfter("://") }
            }
            val path = pathPart?.split('/') ?: emptyList()

            // Extract query parameters
            val queryParams = if (queryStringIndex >= 0) {
                val queryString = url.substring(queryStringIndex + 1)
                queryString.split('&').mapNotNull { param ->
                    val parts = param.split('=', limit = 2)
                    if (parts.size == 2 && parts[0].isNotEmpty()) {
                        PostmanQueryParam(key = parts[0], value = parts[1])
                    } else if (parts.size == 1 && parts[0].isNotEmpty()) {
                        PostmanQueryParam(key = parts[0], value = null)
                    } else {
                        null
                    }
                }
            } else {
                null
            }

            return PostmanUrl(
                raw = url,
                protocol = protocol,
                host = if (host.isNotEmpty() && host.any { it.isNotEmpty() }) host else null,
                path = if (path.isNotEmpty() && path.any { it.isNotEmpty() }) path else null,
                query = if (!queryParams.isNullOrEmpty()) queryParams else null,
                variable = null
            )
        } catch (e: Exception) {
            warnings.add("Failed to parse URL '$url': ${e.message}")
            // Return minimal structure
            return PostmanUrl(
                raw = url,
                protocol = null,
                host = null,
                path = null,
                query = null,
                variable = null
            )
        }
    }

    /**
     * Converts request body to PostmanBody format.
     */
    private fun convertBody(
        httpBody: HttpBody?,
        warnings: MutableList<String>
    ): PostmanBody? {
        if (httpBody == null || httpBody.isEmpty) return null

        val body = httpBody.content
        val contentType = httpBody.type.mimeType

        return when {
            // Check content-type to determine body mode
            contentType.contains("multipart/form-data") -> {
                // Note: Full multipart export not implemented, fall back to raw
                PostmanBody(
                    mode = "raw",
                    raw = body,
                    formdata = null,
                    urlencoded = null
                )
            }
            contentType.contains("application/x-www-form-urlencoded") -> {
                // Parse as form-urlencoded based on content-type
                try {
                    val params = body.split('&').mapNotNull { param ->
                        val parts = param.split('=', limit = 2)
                        if (parts.size == 2 && parts[0].isNotEmpty()) {
                            PostmanUrlEncoded(
                                key = parts[0],
                                value = java.net.URLDecoder.decode(parts[1], "UTF-8"),
                                description = null,
                                disabled = false
                            )
                        } else {
                            null
                        }
                    }
                    PostmanBody(
                        mode = "urlencoded",
                        raw = null,
                        formdata = null,
                        urlencoded = if (params.isNotEmpty()) params else null
                    )
                } catch (e: Exception) {
                    // Fall back to raw
                    warnings.add("Failed to parse body as form-encoded, using raw: ${e.message}")
                    PostmanBody(mode = "raw", raw = body, formdata = null, urlencoded = null)
                }
            }
            // Default to raw for all other content types
            else -> {
                PostmanBody(
                    mode = "raw",
                    raw = body,
                    formdata = null,
                    urlencoded = null
                )
            }
        }
    }

    // ====== Postman Collection Data Classes ======

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
}
