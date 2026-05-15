package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import com.github.dhzhu.amateurpostman.services.JsonService
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.security.SecurityScheme
import java.util.UUID

/**
 * Represents an operation in an OpenAPI path.
 */
private data class OperationInfo(
    val path: String,
    val method: PathItem.HttpMethod,
    val operation: Operation
)

/**
 * Importer for OpenAPI specifications.
 *
 * Converts OpenAPI 3.x documents to the internal RequestCollection format.
 * Supports automatic grouping by tags and generation of example request bodies.
 */
object OpenApiImporter {

    /**
     * Result of importing an OpenAPI specification.
     *
     * @property collection The imported collection, null if import failed
     * @property warnings Warning messages during import
     * @property error Critical error message if import failed
     */
    data class ImportResult(
        val collection: RequestCollection?,
        val warnings: List<String>,
        val error: String? = null
    ) {
        val isSuccess: Boolean get() = collection != null

        companion object {
            fun success(collection: RequestCollection, warnings: List<String> = emptyList()) =
                ImportResult(collection, warnings, null)

            fun error(message: String) =
                ImportResult(null, emptyList(), message)
        }
    }

    /**
     * Imports an OpenAPI specification to a RequestCollection.
     *
     * @param openAPI The parsed OpenAPI specification
     * @return ImportResult containing the collection or error details
     */
    fun import(openAPI: OpenAPI): ImportResult {
        val warnings = mutableListOf<String>()

        try {
            // Extract collection info
            val name = openAPI.info?.title ?: "Untitled Collection"
            val description = openAPI.info?.description ?: ""
            val version = openAPI.info?.version ?: ""

            // Extract variables from servers
            val variables = extractVariables(openAPI, warnings)

            // Convert paths to collection items
            val items = convertPaths(openAPI, warnings)

            val collection = RequestCollection(
                id = UUID.randomUUID().toString(),
                name = name,
                description = if (version.isNotBlank()) "$description\n\nVersion: $version" else description,
                items = items,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                variables = variables
            )

            return ImportResult.success(collection, warnings)

        } catch (e: Exception) {
            return ImportResult.error("Failed to import OpenAPI: ${e.message}")
        }
    }

    /**
     * Imports from a parse result.
     */
    fun importFromParseResult(parseResult: OpenApiParser.ParseResult): ImportResult {
        if (!parseResult.isSuccess) {
            return ImportResult.error(parseResult.error ?: "Failed to parse OpenAPI")
        }

        val result = import(parseResult.openAPI!!)
        return if (parseResult.messages.isNotEmpty()) {
            ImportResult(result.collection, result.warnings + parseResult.messages, result.error)
        } else {
            result
        }
    }

    /**
     * Extracts variables from OpenAPI servers configuration.
     */
    private fun extractVariables(openAPI: OpenAPI, warnings: MutableList<String>): List<Variable> {
        val variables = mutableListOf<Variable>()

        // Extract base URL from first server
        openAPI.servers?.firstOrNull()?.let { server ->
            val baseUrl = server.url ?: return@let
            variables.add(Variable(
                key = "baseUrl",
                value = baseUrl,
                description = server.description ?: "Base URL from OpenAPI spec"
            ))
        }

        // Extract security scheme suggestions
        openAPI.components?.securitySchemes?.forEach { (name, scheme) ->
            when (scheme?.type) {
                SecurityScheme.Type.APIKEY -> {
                    val keyName = scheme.name ?: name
                    variables.add(Variable(
                        key = "${keyName}_key",
                        value = "",
                        description = "API Key for $name (${scheme.`in` ?: "header"})"
                    ))
                }
                SecurityScheme.Type.HTTP -> {
                    if (scheme.scheme == "bearer") {
                        variables.add(Variable(
                            key = "bearer_token",
                            value = "",
                            description = "Bearer token for $name"
                        ))
                    }
                }
                SecurityScheme.Type.OAUTH2 -> {
                    variables.add(Variable(
                        key = "oauth_token",
                        value = "",
                        description = "OAuth2 token for $name"
                    ))
                }
                SecurityScheme.Type.OPENIDCONNECT -> {
                    variables.add(Variable(
                        key = "openid_token",
                        value = "",
                        description = "OpenID Connect token for $name"
                    ))
                }
                SecurityScheme.Type.MUTUALTLS, null -> {
                    // No variable needed
                }
            }
        }

        return variables
    }

    /**
     * Converts OpenAPI paths to collection items.
     * Groups by tags, creates folders for each tag.
     */
    private fun convertPaths(openAPI: OpenAPI, warnings: MutableList<String>): List<CollectionItem> {
        val paths = openAPI.paths ?: return emptyList()

        // Group operations by tags
        val tagGroups = mutableMapOf<String, MutableList<OperationInfo>>()

        paths.forEach { (path, pathItem) ->
            pathItem?.readOperationsMap()?.forEach { (method, operation) ->
                if (operation != null) {
                    val tags = operation.tags?.takeIf { it.isNotEmpty() } ?: listOf("Default")
                    tags.forEach { tag ->
                        tagGroups.getOrPut(tag) { mutableListOf() }
                            .add(OperationInfo(path, method, operation))
                    }
                }
            }
        }

        // Create folders for each tag
        return tagGroups.map { (tag, operations) ->
            val children = operations.mapNotNull { info ->
                convertOperation(openAPI, info.path, info.method, info.operation, warnings)
            }

            CollectionItem.Folder(
                id = UUID.randomUUID().toString(),
                name = tag,
                children = children
            )
        }
    }

    /**
     * Converts a single OpenAPI operation to a collection request item.
     */
    private fun convertOperation(
        openAPI: OpenAPI,
        path: String,
        method: PathItem.HttpMethod,
        operation: Operation?,
        warnings: MutableList<String>
    ): CollectionItem.Request? {
        if (operation == null) return null

        try {
            // Build request name and description
            val name = operation.summary ?: "${method.name} $path"
            val description = operation.description ?: ""

            // Build URL (baseUrl + path)
            val baseUrl = openAPI.servers?.firstOrNull()?.url ?: ""
            val url = buildUrl(baseUrl, path, operation.parameters, warnings)

            // Extract headers from parameters
            val headers = extractHeaders(operation.parameters, warnings)

            // Build request body
            val body = operation.requestBody?.let { rb ->
                buildRequestBody(openAPI, rb, warnings)
            }

            // Create HTTP request
            val httpRequest = HttpRequest(
                url = url,
                method = convertHttpMethod(method),
                headers = headers,
                body = body
            )

            return CollectionItem.Request(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                request = httpRequest
            )

        } catch (e: Exception) {
            warnings.add("Failed to convert operation ${method.name} $path: ${e.message}")
            return null
        }
    }

    /**
     * Builds the URL with path parameters and query string.
     */
    private fun buildUrl(
        baseUrl: String,
        path: String,
        parameters: List<Parameter>?,
        warnings: MutableList<String>
    ): String {
        var url = if (baseUrl.isNotBlank()) {
            val normalizedBase = baseUrl.trimEnd('/')
            val normalizedPath = if (path.startsWith("/")) path else "/$path"
            "$normalizedBase$normalizedPath"
        } else {
            path
        }

        // Handle query parameters
        parameters?.filter { it.`in` == "query" }?.takeIf { it.isNotEmpty() }?.let { queryParams ->
            val queryString = queryParams.mapNotNull { param ->
                val key = param.name ?: return@mapNotNull null
                val value = param.example?.toString() ?: param.schema?.example?.toString() ?: ""
                "$key=$value"
            }.joinToString("&")

            if (queryString.isNotBlank()) {
                url = "$url?$queryString"
            }
        }

        // Handle path parameters (replace {param} placeholders)
        parameters?.filter { it.`in` == "path" }?.forEach { param ->
            val placeholder = "{${param.name}}"
            val example = param.example?.toString() ?: param.schema?.example?.toString() ?: placeholder
            url = url.replace(placeholder, example)
        }

        return url
    }

    /**
     * Extracts headers from parameters.
     */
    private fun extractHeaders(parameters: List<Parameter>?, warnings: MutableList<String>): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        parameters?.filter { it.`in` == "header" }?.forEach { param ->
            val name = param.name ?: return@forEach
            val value = param.example?.toString() ?: param.schema?.example?.toString() ?: ""
            headers[name] = value
        }

        return headers
    }

    /**
     * Builds the request body from OpenAPI requestBody definition.
     */
    private fun buildRequestBody(
        openAPI: OpenAPI,
        requestBody: RequestBody,
        warnings: MutableList<String>
    ): HttpBody? {
        val content = requestBody.content ?: return null

        // Try JSON first, then other content types
        val mediaType = content["application/json"]
            ?: content["application/xml"]
            ?: content.entries.firstOrNull()?.value
            ?: return null

        val contentType = content.entries.firstOrNull { it.value == mediaType }?.key
            ?: "application/json"

        val bodyType = when {
            contentType.contains("json", ignoreCase = true) -> BodyType.JSON
            contentType.contains("xml", ignoreCase = true) -> BodyType.XML
            contentType.contains("form-urlencoded", ignoreCase = true) -> BodyType.FORM_URLENCODED
            contentType.contains("multipart", ignoreCase = true) -> BodyType.MULTIPART
            else -> BodyType.JSON
        }

        val schema = mediaType.schema ?: return null
        val exampleContent = generateExampleFromSchema(schema, openAPI)

        return HttpBody(exampleContent, bodyType)
    }

    /**
     * Generates example content from a schema definition.
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateExampleFromSchema(schema: Schema<*>, openAPI: OpenAPI): String {
        // Check for explicit example
        schema.example?.let { example ->
            return when (example) {
                is String -> if (schema is StringSchema) "\"$example\"" else example.toString()
                is Number, is Boolean -> example.toString()
                is Map<*, *> -> JsonService.mapper.writeValueAsString(example)
                is List<*> -> JsonService.mapper.writeValueAsString(example)
                else -> example.toString()
            }
        }

        // Check for example in media type
        if (schema is ObjectSchema || schema.properties?.isNotEmpty() == true) {
            return generateObjectExample(schema, openAPI)
        }

        // Generate based on type
        return when (schema) {
            is StringSchema -> "\"${schema.format ?: "string"}\""
            is IntegerSchema -> (schema.example ?: 0).toString()
            is BooleanSchema -> (schema.example ?: false).toString()
            is ArraySchema -> {
                val itemExample = schema.items?.let { generateExampleFromSchema(it, openAPI) } ?: "\"item\""
                "[$itemExample]"
            }
            else -> "{}"
        }
    }

    /**
     * Generates example JSON for an object schema.
     */
    private fun generateObjectExample(schema: Schema<*>, openAPI: OpenAPI): String {
        val properties = schema.properties ?: return "{}"
        val example = mutableMapOf<String, Any?>()

        properties.forEach { (name, propSchema) ->
            val value = when (propSchema) {
                is StringSchema -> propSchema.example ?: (propSchema.format ?: "string")
                is IntegerSchema -> propSchema.example ?: 0
                is BooleanSchema -> propSchema.example ?: false
                is ArraySchema -> {
                    propSchema.items?.example ?: emptyList<Any>()
                }
                is ObjectSchema -> {
                    // Nested object
                    parseJsonToObject(generateObjectExample(propSchema, openAPI))
                }
                else -> propSchema?.example ?: null
            }
            example[name] = value
        }

        return JsonService.mapper.writeValueAsString(example)
    }

    /**
     * Parses JSON string to object for nested structures.
     */
    private fun parseJsonToObject(json: String): Any? {
        return try {
            JsonService.mapper.readValue(json, Any::class.java)
        } catch (e: Exception) {
            json
        }
    }

    /**
     * Converts OpenAPI HttpMethod enum to internal HttpMethod enum.
     */
    private fun convertHttpMethod(method: PathItem.HttpMethod): HttpMethod {
        return when (method) {
            PathItem.HttpMethod.GET -> HttpMethod.GET
            PathItem.HttpMethod.POST -> HttpMethod.POST
            PathItem.HttpMethod.PUT -> HttpMethod.PUT
            PathItem.HttpMethod.DELETE -> HttpMethod.DELETE
            PathItem.HttpMethod.PATCH -> HttpMethod.PATCH
            PathItem.HttpMethod.HEAD -> HttpMethod.HEAD
            PathItem.HttpMethod.OPTIONS -> HttpMethod.OPTIONS
            PathItem.HttpMethod.TRACE -> HttpMethod.GET // TRACE not supported, fallback
        }
    }
}