package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import com.github.dhzhu.amateurpostman.services.RequestHistoryService
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.tags.Tag
import java.io.File
import java.net.URI

/**
 * Exporter for OpenAPI 3.0.3 format.
 *
 * Converts internal collections to OpenAPI 3.0.3 YAML or JSON format using
 * the official swagger-models library for standards compliance.
 * Mapping rules:
 * - Collection name  → info.title
 * - Collection desc  → info.description
 * - Folder hierarchy → tags (full path, e.g. "users/admin")
 * - Top-level reqs   → tagged with the collection name
 * - {{variable}}     → {variable} path parameters
 * - Same path + diff method → merged PathItem
 * - Request headers  → HeaderParameter (sensitive ones filtered by default)
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

    private val SENSITIVE_HEADERS = setOf("authorization", "cookie", "set-cookie")

    // ── Public API ────────────────────────────────────────────────────────────

    fun exportCollection(
        collection: RequestCollection,
        format: ExportFormat,
        includeSensitiveHeaders: Boolean = false,
        historyService: RequestHistoryService? = null
    ): ExportResult {
        val warnings = mutableListOf<String>()
        return try {
            val doc = buildOpenApiDoc(collection, warnings, includeSensitiveHeaders, historyService)
            val content = when (format) {
                ExportFormat.JSON -> Json.pretty(doc)
                ExportFormat.YAML -> Yaml.pretty(doc)
            }
            ExportResult.success(content, warnings)
        } catch (e: Exception) {
            ExportResult.error("Failed to export collection: ${e.message}")
        }
    }

    fun exportToFile(
        collection: RequestCollection,
        file: File,
        format: ExportFormat,
        includeSensitiveHeaders: Boolean = false,
        historyService: RequestHistoryService? = null
    ): ExportResult {
        val result = exportCollection(collection, format, includeSensitiveHeaders, historyService)
        if (!result.isSuccess) return result
        return try {
            file.writeText(result.content ?: "")
            result
        } catch (e: Exception) {
            ExportResult.error("Failed to write file: ${e.message}")
        }
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private fun buildOpenApiDoc(
        collection: RequestCollection,
        warnings: MutableList<String>,
        includeSensitiveHeaders: Boolean,
        historyService: RequestHistoryService?
    ): OpenAPI {
        val paths = Paths()
        val tags = linkedSetOf<String>()
        val usedOperationIds = mutableSetOf<String>()

        collection.items.forEach { item ->
            processItem(
                item, "", collection.name, paths, tags,
                warnings, usedOperationIds, includeSensitiveHeaders, historyService
            )
        }

        val openAPI = OpenAPI()
        openAPI.openapi = "3.0.3"
        openAPI.info = Info()
            .title(collection.name)
            .description(collection.description.ifEmpty { null })
            .version("1.0.0")

        if (tags.isNotEmpty()) openAPI.tags = tags.map { Tag().name(it) }
        if (paths.isNotEmpty()) openAPI.paths = paths

        return openAPI
    }

    private fun processItem(
        item: CollectionItem,
        tagPath: String,
        defaultTag: String,
        paths: Paths,
        tags: MutableSet<String>,
        warnings: MutableList<String>,
        usedOperationIds: MutableSet<String>,
        includeSensitiveHeaders: Boolean,
        historyService: RequestHistoryService?
    ) {
        when (item) {
            is CollectionItem.Folder -> {
                val newTagPath = if (tagPath.isEmpty()) item.name else "$tagPath/${item.name}"
                item.children.forEach { child ->
                    processItem(
                        child, newTagPath, defaultTag, paths, tags,
                        warnings, usedOperationIds, includeSensitiveHeaders, historyService
                    )
                }
            }
            is CollectionItem.Request -> {
                val tag = if (tagPath.isNotEmpty()) tagPath else defaultTag
                tags.add(tag)

                val openApiPath = extractOpenApiPath(item.request.url)
                val httpMethod = PathItem.HttpMethod.valueOf(item.request.method.name)

                val existingPathItem = paths[openApiPath]
                if (existingPathItem?.readOperationsMap()?.containsKey(httpMethod) == true) {
                    warnings.add("Duplicate ${item.request.method.name} $openApiPath — skipping '${item.name}'")
                    return
                }

                val operation = buildOperation(
                    item, tag, openApiPath, warnings, usedOperationIds,
                    includeSensitiveHeaders, historyService
                )
                val pathItem = existingPathItem ?: PathItem()
                pathItem.operation(httpMethod, operation)
                paths.addPathItem(openApiPath, pathItem)
            }
        }
    }

    private fun buildOperation(
        item: CollectionItem.Request,
        tag: String,
        openApiPath: String,
        warnings: MutableList<String>,
        usedOperationIds: MutableSet<String>,
        includeSensitiveHeaders: Boolean,
        historyService: RequestHistoryService?
    ): Operation {
        val request = item.request
        val parameters = mutableListOf<Parameter>()
        parameters.addAll(extractPathParams(openApiPath))
        parameters.addAll(extractQueryParams(request.url))
        parameters.addAll(extractHeaderParams(request.headers, includeSensitiveHeaders))

        val requestBody = if (request.method == HttpMethod.GET || request.method == HttpMethod.HEAD) {
            null
        } else {
            buildRequestBody(request.body, warnings)
        }

        val operationId = uniqueOperationId(
            "${request.method.name.lowercase()}${openApiPath.toOperationIdSuffix()}",
            usedOperationIds
        )

        val responses = buildResponses(item, historyService)

        val operation = Operation()
            .summary(item.name)
            .operationId(operationId)
            .addTagsItem(tag)
            .responses(responses)

        if (parameters.isNotEmpty()) operation.parameters = parameters
        if (requestBody != null) operation.requestBody = requestBody

        return operation
    }

    // ── Header helpers ────────────────────────────────────────────────────────

    private fun extractHeaderParams(
        headers: Map<String, String>,
        includeSensitive: Boolean
    ): List<Parameter> = headers.mapNotNull { (name, _) ->
        if (!includeSensitive && name.lowercase() in SENSITIVE_HEADERS) return@mapNotNull null
        HeaderParameter()
            .name(name)
            .required(false)
            .schema(Schema<String>().type("string"))
    }

    // ── Response helpers ──────────────────────────────────────────────────────

    private fun buildResponses(
        item: CollectionItem.Request,
        historyService: RequestHistoryService?
    ): ApiResponses {
        val responses = ApiResponses()

        if (historyService != null) {
            val matchingResponse = historyService.getHistory().firstOrNull { entry ->
                entry.request.url == item.request.url &&
                    entry.request.method == item.request.method &&
                    entry.response?.isSuccessful == true
            }?.response

            if (matchingResponse != null) {
                val statusCode = matchingResponse.statusCode.toString()
                val description = matchingResponse.statusMessage.ifEmpty { "OK" }
                val apiResponse = ApiResponse().description(description)
                val schema = JsonToSchemaConverter.convert(matchingResponse.body)
                if (schema != null) {
                    val content = Content()
                    content.addMediaType("application/json", MediaType().schema(schema))
                    apiResponse.content = content
                }
                responses.addApiResponse(statusCode, apiResponse)
                return responses
            }
        }

        responses.addApiResponse("200", ApiResponse().description("OK"))
        return responses
    }

    // ── Request body helpers ──────────────────────────────────────────────────

    private fun buildRequestBody(body: HttpBody?, warnings: MutableList<String>): RequestBody? {
        if (body == null || body.isEmpty) return null

        val content = Content()
        when (body.type) {
            BodyType.FORM_URLENCODED -> {
                val props = parseUrlencodedBody(body.content)
                val schema = Schema<Any>().type("object")
                if (props.isNotEmpty()) schema.properties = props.mapValues { Schema<String>().type("string") }
                content.addMediaType("application/x-www-form-urlencoded", MediaType().schema(schema))
            }
            BodyType.MULTIPART -> {
                val schema = Schema<Any>().type("object")
                val props = buildMultipartProperties(body.multipartData)
                if (props.isNotEmpty()) schema.properties = props
                content.addMediaType("multipart/form-data", MediaType().schema(schema))
            }
            else -> {
                val mimeType = body.type.mimeType.ifEmpty { "application/octet-stream" }
                content.addMediaType(mimeType, MediaType().schema(Schema<Any>().type("object")))
            }
        }

        return RequestBody().required(true).content(content)
    }

    private fun parseUrlencodedBody(content: String): Map<String, String> {
        if (content.isBlank()) return emptyMap()
        return content.split('&').mapNotNull { part ->
            val key = java.net.URLDecoder.decode(
                part.substringBefore('=').trim(), "UTF-8"
            ).trim()
            if (key.isEmpty()) null else key to "string"
        }.toMap()
    }

    private fun buildMultipartProperties(parts: List<MultipartPart>?): Map<String, Schema<*>> {
        if (parts.isNullOrEmpty()) return emptyMap()
        return parts.associate { part ->
            when (part) {
                is MultipartPart.TextField -> part.key to Schema<String>().type("string")
                is MultipartPart.FileField -> part.key to Schema<String>().type("string").format("binary")
            }
        }
    }

    // ── URL helpers ───────────────────────────────────────────────────────────

    internal fun extractOpenApiPath(rawUrl: String): String {
        val stdUrl = rawUrl.replace(Regex("\\{\\{([^}]+)}}"), "{$1}")
        return try {
            val encoded = stdUrl
                .replace("{", "%7B")
                .replace("}", "%7D")
            val path = URI(encoded).path
                .replace("%7B", "{")
                .replace("%7D", "}")
                .substringBefore("?")
            if (path.isEmpty()) "/" else path
        } catch (e: Exception) {
            val withoutProto = stdUrl.substringAfter("://", stdUrl)
            val slashIdx = withoutProto.indexOf('/')
            if (slashIdx < 0) "/" else withoutProto.substring(slashIdx).substringBefore("?")
        }
    }

    private fun extractPathParams(openApiPath: String): List<Parameter> =
        Regex("\\{([^}]+)}")
            .findAll(openApiPath)
            .map {
                PathParameter()
                    .name(it.groupValues[1])
                    .required(true)
                    .schema(Schema<String>().type("string"))
            }
            .toList()

    private fun extractQueryParams(rawUrl: String): List<Parameter> {
        val qIdx = rawUrl.indexOf('?')
        if (qIdx < 0) return emptyList()
        return rawUrl.substring(qIdx + 1).split('&').mapNotNull { part ->
            val name = part.substringBefore('=').trim()
            if (name.isEmpty() || name.startsWith("{{")) null
            else QueryParameter()
                .name(name)
                .required(false)
                .schema(Schema<String>().type("string"))
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
}
