package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.net.URI

/**
 * Exporter for OpenAPI 3.0.3 format.
 *
 * Converts internal collections to OpenAPI 3.0.3 YAML or JSON format.
 * Mapping rules:
 * - Collection name  → info.title
 * - Collection desc  → info.description
 * - Folder hierarchy → tags (full path, e.g. "users/admin")
 * - Top-level reqs   → tagged with the collection name
 * - {{variable}}     → {variable} path parameters
 * - Same path + diff method → merged PathItem
 */
object OpenApiExporter {

    enum class ExportFormat(val extension: String, val displayName: String) {
        YAML("yaml", "YAML"),
        JSON("json", "JSON")
    }

    data class ExportResult(
        val content: String?,
        val warnings: List<String>,
        val error: String? = null
    ) {
        companion object {
            fun success(content: String, warnings: List<String> = emptyList()) =
                ExportResult(content, warnings)

            fun error(message: String) = ExportResult(null, emptyList(), message)
        }

        val isSuccess: Boolean get() = content != null
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val yaml: Yaml by lazy {
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            indent = 2
            isPrettyFlow = true
        }
        Yaml(options)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun exportCollection(collection: RequestCollection, format: ExportFormat): ExportResult {
        val warnings = mutableListOf<String>()
        return try {
            val doc = buildOpenApiDoc(collection, warnings)
            val content = when (format) {
                ExportFormat.JSON -> serializeToJson(doc)
                ExportFormat.YAML -> serializeToYaml(doc)
            }
            ExportResult.success(content, warnings)
        } catch (e: Exception) {
            ExportResult.error("Failed to export collection: ${e.message}")
        }
    }

    fun exportToFile(collection: RequestCollection, file: File, format: ExportFormat): ExportResult {
        val result = exportCollection(collection, format)
        if (!result.isSuccess) return result
        return try {
            file.writeText(result.content ?: "")
            result
        } catch (e: Exception) {
            ExportResult.error("Failed to write file: ${e.message}")
        }
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private fun buildOpenApiDoc(collection: RequestCollection, warnings: MutableList<String>): OpenApiDoc {
        // paths: url-path → (method → Operation)
        val paths = linkedMapOf<String, MutableMap<String, Operation>>()
        val tags = linkedSetOf<String>()
        val usedOperationIds = mutableSetOf<String>()

        collection.items.forEach { item ->
            processItem(item, "", collection.name, paths, tags, warnings, usedOperationIds)
        }

        return OpenApiDoc(
            info = Info(
                title = collection.name,
                description = collection.description.ifEmpty { null }
            ),
            tags = if (tags.isEmpty()) null else tags.map { TagObject(name = it) },
            paths = if (paths.isEmpty()) null else paths
        )
    }

    private fun processItem(
        item: CollectionItem,
        tagPath: String,
        defaultTag: String,
        paths: MutableMap<String, MutableMap<String, Operation>>,
        tags: MutableSet<String>,
        warnings: MutableList<String>,
        usedOperationIds: MutableSet<String>
    ) {
        when (item) {
            is CollectionItem.Folder -> {
                val newTagPath = if (tagPath.isEmpty()) item.name else "$tagPath/${item.name}"
                item.children.forEach { child ->
                    processItem(child, newTagPath, defaultTag, paths, tags, warnings, usedOperationIds)
                }
            }
            is CollectionItem.Request -> {
                val tag = if (tagPath.isNotEmpty()) tagPath else defaultTag
                tags.add(tag)

                val openApiPath = extractOpenApiPath(item.request.url)
                val method = item.request.method.name.lowercase()

                val existingEntry = paths[openApiPath]
                if (existingEntry != null && existingEntry.containsKey(method)) {
                    warnings.add("Duplicate $method $openApiPath — skipping '${item.name}'")
                    return
                }

                val operation = buildOperation(item, tag, openApiPath, warnings, usedOperationIds)
                paths.getOrPut(openApiPath) { linkedMapOf() }[method] = operation
            }
        }
    }

    private fun buildOperation(
        item: CollectionItem.Request,
        tag: String,
        openApiPath: String,
        warnings: MutableList<String>,
        usedOperationIds: MutableSet<String>
    ): Operation {
        val request = item.request
        val pathParams = extractPathParams(openApiPath)
        val queryParams = extractQueryParams(request.url)
        val parameters = (pathParams + queryParams).ifEmpty { null }

        val requestBody = if (request.method == HttpMethod.GET || request.method == HttpMethod.HEAD) {
            null
        } else {
            buildRequestBody(request.body, warnings)
        }

        val operationId = uniqueOperationId(
            "${request.method.name.lowercase()}${openApiPath.toOperationIdSuffix()}",
            usedOperationIds
        )

        return Operation(
            summary = item.name,
            operationId = operationId,
            tags = listOf(tag),
            parameters = parameters,
            requestBody = requestBody,
            responses = linkedMapOf("200" to mapOf("description" to "OK"))
        )
    }

    private fun buildRequestBody(body: HttpBody?, warnings: MutableList<String>): RequestBody? {
        if (body == null || body.isEmpty) return null

        val content: Map<String, Any> = when (body.type) {
            BodyType.FORM_URLENCODED -> {
                val properties = parseUrlencodedBody(body.content)
                mapOf("application/x-www-form-urlencoded" to schemaWithProperties(properties))
            }
            BodyType.MULTIPART -> {
                val properties = buildMultipartProperties(body.multipartData)
                mapOf("multipart/form-data" to schemaWithProperties(properties))
            }
            else -> {
                val mimeType = body.type.mimeType.ifEmpty { "application/octet-stream" }
                mapOf(mimeType to mapOf("schema" to mapOf("type" to "object")))
            }
        }

        return RequestBody(required = true, content = content)
    }

    private fun parseUrlencodedBody(content: String): Map<String, Map<String, String>> {
        if (content.isBlank()) return emptyMap()
        return content.split('&').mapNotNull { part ->
            val key = java.net.URLDecoder.decode(
                part.substringBefore('=').trim(), "UTF-8"
            ).trim()
            if (key.isEmpty()) null else key to mapOf("type" to "string")
        }.toMap()
    }

    private fun buildMultipartProperties(parts: List<MultipartPart>?): Map<String, Map<String, String>> {
        if (parts.isNullOrEmpty()) return emptyMap()
        return parts.associate { part ->
            when (part) {
                is MultipartPart.TextField -> part.key to mapOf("type" to "string")
                is MultipartPart.FileField -> part.key to mapOf("type" to "string", "format" to "binary")
            }
        }
    }

    private fun schemaWithProperties(properties: Map<String, Any>): Map<String, Any> {
        val schema: MutableMap<String, Any> = linkedMapOf("type" to "object")
        if (properties.isNotEmpty()) schema["properties"] = properties
        return mapOf("schema" to schema)
    }

    // ── URL helpers ───────────────────────────────────────────────────────────

    internal fun extractOpenApiPath(rawUrl: String): String {
        // Convert {{varName}} → {varName} first
        val stdUrl = rawUrl.replace(Regex("\\{\\{([^}]+)}}"), "{$1}")
        return try {
            // Temporarily encode braces so URI can parse
            val encoded = stdUrl
                .replace("{", "%7B")
                .replace("}", "%7D")
            val path = URI(encoded).path
                .replace("%7B", "{")
                .replace("%7D", "}")
                .substringBefore("?")
            if (path.isEmpty()) "/" else path
        } catch (e: Exception) {
            // Fallback manual extraction
            val withoutProto = stdUrl.substringAfter("://", stdUrl)
            val slashIdx = withoutProto.indexOf('/')
            if (slashIdx < 0) "/" else withoutProto.substring(slashIdx).substringBefore("?")
        }
    }

    private fun extractPathParams(openApiPath: String): List<Parameter> =
        Regex("\\{([^}]+)}")
            .findAll(openApiPath)
            .map { Parameter(name = it.groupValues[1], location = "path", required = true) }
            .toList()

    private fun extractQueryParams(rawUrl: String): List<Parameter> {
        val qIdx = rawUrl.indexOf('?')
        if (qIdx < 0) return emptyList()
        return rawUrl.substring(qIdx + 1).split('&').mapNotNull { part ->
            val name = part.substringBefore('=').trim()
            if (name.isEmpty() || name.startsWith("{{")) null
            else Parameter(name = name, location = "query", required = false)
        }
    }

    private fun String.toOperationIdSuffix(): String =
        split('/')
            .filter { it.isNotEmpty() && !it.startsWith('{') }
            .joinToString("") { seg -> seg.replaceFirstChar { it.uppercaseChar() } }
            .ifEmpty { "Root" }

    private fun uniqueOperationId(base: String, used: MutableSet<String>): String {
        if (used.add(base)) return base
        var counter = 2
        while (true) {
            val candidate = "$base$counter"
            if (used.add(candidate)) return candidate
            counter++
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private fun serializeToJson(doc: OpenApiDoc): String = gson.toJson(doc)

    private fun serializeToYaml(doc: OpenApiDoc): String {
        // Convert via JSON so Gson handles field naming / null exclusion
        val map = gson.fromJson(gson.toJson(doc), Any::class.java)
        return yaml.dump(map)
    }

    // ── Private data classes (OpenAPI 3.0.3 structure) ────────────────────────

    private data class OpenApiDoc(
        val openapi: String = "3.0.3",
        val info: Info,
        val tags: List<TagObject>? = null,
        val paths: Map<String, Any>? = null
    )

    private data class Info(
        val title: String,
        val description: String? = null,
        val version: String = "1.0.0"
    )

    private data class TagObject(val name: String)

    private data class Operation(
        val summary: String,
        val operationId: String,
        val tags: List<String>,
        val parameters: List<Parameter>? = null,
        val requestBody: RequestBody? = null,
        val responses: Map<String, Any>
    )

    private data class Parameter(
        val name: String,
        @SerializedName("in") val location: String,
        val required: Boolean,
        val schema: Map<String, String> = mapOf("type" to "string")
    )

    private data class RequestBody(
        val required: Boolean,
        val content: Map<String, Any>
    )
}
