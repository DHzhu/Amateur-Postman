package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import java.net.URI
import java.util.UUID

/**
 * Converts parsed HAR entries into the internal [RequestCollection] model.
 *
 * Responsibilities:
 * - Static resource filtering (images, fonts, CSS, JS, media)
 * - Grouping entries by host into [CollectionItem.Folder]s
 * - Converting each [HarParser.HarEntry] into an [HttpRequest]
 * - Parsing multipart/form-data params into [MultipartPart] lists
 */
object HarConverter {

    // MIME type prefixes that indicate non-API static resources
    private val STATIC_MIME_PREFIXES = listOf("image/", "font/", "audio/", "video/")

    // Exact MIME types considered static
    private val STATIC_MIME_EXACT = setOf(
        "text/css",
        "text/javascript",
        "application/javascript",
        "application/x-javascript"
    )

    // Chrome _resourceType values considered static
    private val STATIC_RESOURCE_TYPES = setOf("image", "font", "stylesheet", "media")

    // ── Result type (mirrors PostmanImporter.ImportResult) ────────────────────

    data class ImportResult(
        val collection: RequestCollection?,
        val warnings: List<String>,
        val error: String? = null
    ) {
        val isSuccess: Boolean get() = collection != null

        companion object {
            fun success(collection: RequestCollection, warnings: List<String> = emptyList()) =
                ImportResult(collection, warnings)

            fun error(message: String) = ImportResult(null, emptyList(), message)
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns true if [entry] is a static resource (image, font, CSS, JS, media).
     */
    fun isStaticResource(entry: HarParser.HarEntry): Boolean {
        val resourceType = entry._resourceType?.lowercase()
        if (resourceType != null && resourceType in STATIC_RESOURCE_TYPES) return true

        val mimeType = entry.response.content.mimeType.lowercase()
        if (STATIC_MIME_PREFIXES.any { mimeType.startsWith(it) }) return true
        if (mimeType in STATIC_MIME_EXACT) return true

        return false
    }

    /**
     * Groups [entries] by their host, optionally filtering static resources first.
     *
     * @return Map of host → list of entries, ordered by first appearance.
     */
    fun groupByHost(
        entries: List<HarParser.HarEntry>,
        filterStatic: Boolean = true
    ): Map<String, List<HarParser.HarEntry>> {
        val filtered = if (filterStatic) entries.filter { !isStaticResource(it) } else entries
        return filtered.groupBy { entry ->
            try {
                URI(entry.request.url).host?.lowercase() ?: "unknown"
            } catch (_: Exception) {
                "unknown"
            }
        }
    }

    /**
     * Converts [selectedEntries] into a [RequestCollection] named [collectionName].
     *
     * Entries are always re-grouped by host. When only one host is present the
     * folder wrapper is omitted and requests are placed at the top level.
     */
    fun toCollection(
        collectionName: String,
        selectedEntries: List<HarParser.HarEntry>
    ): ImportResult {
        if (collectionName.isBlank()) return ImportResult.error("Collection name cannot be empty")
        if (selectedEntries.isEmpty()) return ImportResult.error("No entries selected")

        val warnings = mutableListOf<String>()
        // Re-group without static filter (caller already chose which entries to include)
        val groups = groupByHost(selectedEntries, filterStatic = false)
        val items = mutableListOf<CollectionItem>()
        val singleHost = groups.size == 1

        groups.forEach { (host, hostEntries) ->
            val children = mutableListOf<CollectionItem>()
            hostEntries.forEach { entry ->
                try {
                    val request = toHttpRequest(entry, warnings)
                    val path = try {
                        URI(entry.request.url).path?.takeIf { it.isNotBlank() } ?: "/"
                    } catch (_: Exception) {
                        entry.request.url
                    }
                    children.add(
                        CollectionItem.Request(
                            id = UUID.randomUUID().toString(),
                            name = "${entry.request.method} $path",
                            request = request
                        )
                    )
                } catch (e: Exception) {
                    warnings.add("Skipped '${entry.request.url}': ${e.message}")
                }
            }

            if (children.isNotEmpty()) {
                if (singleHost) {
                    items.addAll(children)
                } else {
                    items.add(
                        CollectionItem.Folder(
                            id = UUID.randomUUID().toString(),
                            name = host,
                            children = children
                        )
                    )
                }
            }
        }

        val collection = RequestCollection(
            id = UUID.randomUUID().toString(),
            name = collectionName,
            description = "Imported from HAR file",
            items = items,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )

        return ImportResult.success(collection, warnings)
    }

    /**
     * Converts a single HAR entry into an [HttpRequest].
     *
     * - HTTP/2 pseudo-headers (`:authority`, `:method`, etc.) are skipped.
     * - multipart/form-data params are parsed into [MultipartPart] lists.
     * - application/x-www-form-urlencoded params are serialised to `key=value&…`.
     */
    fun toHttpRequest(
        entry: HarParser.HarEntry,
        warnings: MutableList<String> = mutableListOf()
    ): HttpRequest {
        val harReq = entry.request

        val method = try {
            HttpMethod.valueOf(harReq.method.uppercase())
        } catch (_: IllegalArgumentException) {
            warnings.add("Unknown HTTP method '${harReq.method}', defaulting to GET")
            HttpMethod.GET
        }

        val headers = harReq.headers
            .filter { !it.name.startsWith(":") && it.name.isNotBlank() }
            .groupBy { it.name }
            .mapValues { (_, values) -> values.joinToString(", ") { it.value } }

        val body = convertBody(harReq.postData, warnings)

        return HttpRequest(
            url = harReq.url,
            method = method,
            headers = headers,
            body = body
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun convertBody(
        postData: HarParser.HarPostData?,
        warnings: MutableList<String>
    ): HttpBody? {
        if (postData == null) return null

        val mime = postData.mimeType.lowercase()

        return when {
            mime.contains("multipart/form-data") -> {
                val parts = postData.params?.map { param ->
                    if (param.fileName != null) {
                        MultipartPart.FileField(
                            key = param.name,
                            filePath = "",
                            fileName = param.fileName,
                            contentType = param.contentType
                        )
                    } else {
                        MultipartPart.TextField(
                            key = param.name,
                            value = param.value ?: "",
                            contentType = param.contentType
                        )
                    }
                } ?: emptyList()

                HttpBody(
                    content = postData.text ?: "",
                    type = BodyType.MULTIPART,
                    multipartData = parts
                )
            }

            mime.contains("application/x-www-form-urlencoded") -> {
                val content = postData.params
                    ?.joinToString("&") {
                        val k = java.net.URLEncoder.encode(it.name, "UTF-8")
                        val v = java.net.URLEncoder.encode(it.value ?: "", "UTF-8")
                        "$k=$v"
                    }
                    ?: postData.text
                    ?: ""
                HttpBody(content = content, type = BodyType.FORM_URLENCODED)
            }

            else -> {
                val bodyType = BodyType.fromMimeType(postData.mimeType)
                HttpBody(content = postData.text ?: "", type = bodyType)
            }
        }
    }
}
